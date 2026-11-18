package com.intelliroute.service;

import com.intelliroute.model.*;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class RouteOptimizationService {

    private final GeocodingService geocodingService;
    private final RoutingService routingService;
    private final TrafficService trafficService;
    private final WeatherService weatherService;
    private final RouteScoringService routeScoringService;

    public RouteOptimizationService(
            GeocodingService geocodingService,
            RoutingService routingService,
            TrafficService trafficService,
            WeatherService weatherService,
            RouteScoringService routeScoringService
    ) {
        this.geocodingService = geocodingService;
        this.routingService = routingService;
        this.trafficService = trafficService;
        this.weatherService = weatherService;
        this.routeScoringService = routeScoringService;
    }

    public RouteResponse optimizeRoute(RouteRequest request) {
        long startTime = System.currentTimeMillis();

        // 1. Resolve & validate source location inside India
        Coordinate sourceCoord = geocodingService.resolveLocation(
                request.getSource(),
                request.getSourceLat(),
                request.getSourceLon(),
                true
        );

        // 2. Resolve & validate destination location inside India
        Coordinate destCoord = geocodingService.resolveLocation(
                request.getDestination(),
                request.getDestLat(),
                request.getDestLon(),
                false
        );

        // Disallow identical source and destination
        if (Math.abs(sourceCoord.getLatitude() - destCoord.getLatitude()) < 0.0001 &&
            Math.abs(sourceCoord.getLongitude() - destCoord.getLongitude()) < 0.0001) {
            throw new IllegalArgumentException("Source and destination locations cannot be identical.");
        }

        // 3. Generate genuine routes via OSRM
        List<RouteInfo> routes = routingService.calculateRoutes(sourceCoord, destCoord, request.getVehicle());
        if (routes.isEmpty()) {
            throw new IllegalStateException("No viable routes found between selected locations.");
        }

        // 4. Perform Route-Segment-Based Traffic and Time-Aware Weather Analysis for each route
        for (RouteInfo route : routes) {
            List<RouteSegment> segments = createRouteSegments(route);
            route.setSegments(segments);

            // Segment-based traffic analysis (TomTom Flow Segment Absolute Mode)
            TrafficInfo traffic = trafficService.analyzeRouteSegments(segments);
            route.setTraffic(traffic.getCondition());
            route.setTrafficScore(traffic.getScore());
            route.setTrafficDetails(traffic);
            route.setTrafficCoveragePercent(traffic.getCoveragePercent());
            route.setCongestedSegmentCount(traffic.getCongestedSegmentCount());

            // Segment-based time-aware weather analysis (Open-Meteo Hourly Forecast)
            WeatherInfo weather = weatherService.analyzeRouteSegments(segments);
            route.setWeather(weather.getCondition());
            route.setWeatherScore(weather.getScore());
            route.setWeatherDetails(weather);
            route.setWeatherCoveragePercent(weather.getCoveragePercent());
            route.setRainAffectedSegmentCount(weather.getRainAffectedSegmentCount());
        }

        // 5. Multi-factor scoring and ranking according to user preference
        routeScoringService.scoreAndRankRoutes(routes, request.getPreference());

        // 6. Build response
        RouteResponse response = new RouteResponse();
        response.setSource(sourceCoord);
        response.setDestination(destCoord);
        response.setVehicle(request.getVehicle());
        response.setPreference(request.getPreference());
        response.setTotalRoutesFound(routes.size());
        response.setRoutes(routes);
        response.setRecommendedRoute(!routes.isEmpty() ? routes.get(0) : null);
        response.setExecutionTimeMs(System.currentTimeMillis() - startTime);

        RouteInfo winner = routes.get(0);
        String summary = String.format("Evaluated %d candidate route%s across the corridor. Recommended: %s (%s, Score: %.1f/100).",
                routes.size(),
                routes.size() > 1 ? "s" : "",
                winner.getId(),
                winner.getRouteName(),
                winner.getScore());
        response.setSummary(summary);

        return response;
    }

    /**
     * Decomposes a candidate route into representative road segments for traffic & weather sampling.
     */
    private List<RouteSegment> createRouteSegments(RouteInfo route) {
        List<List<Double>> path = route.getPath();
        List<RouteSegment> segments = new ArrayList<>();

        if (path == null || path.isEmpty()) {
            return segments;
        }

        int coordCount = path.size();
        double totalDistanceKm = route.getDistanceKm();
        double totalMinutes = route.getEstimatedTimeMinutes();

        // Calculate appropriate segment count based on route length
        int targetSegmentCount;
        if (totalDistanceKm < 30.0) {
            targetSegmentCount = Math.min(3, coordCount);
        } else if (totalDistanceKm < 150.0) {
            targetSegmentCount = Math.min(5, coordCount);
        } else if (totalDistanceKm < 500.0) {
            targetSegmentCount = Math.min(8, coordCount);
        } else {
            targetSegmentCount = Math.min(10, coordCount); // Max 10 segments to strictly respect provider limits
        }

        targetSegmentCount = Math.max(1, targetSegmentCount);

        double baseSegDist = totalDistanceKm / targetSegmentCount;

        // Weight segment transit durations according to corridor topography:
        // Departure (urban/suburban) and arrival (city approach) are slower than open highway corridors
        double[] speedFactors = new double[targetSegmentCount];
        double sumWeights = 0.0;
        for (int i = 0; i < targetSegmentCount; i++) {
            if (targetSegmentCount >= 3) {
                if (i == 0) {
                    speedFactors[i] = 0.65; // Urban origin exit
                } else if (i == targetSegmentCount - 1) {
                    speedFactors[i] = 0.55; // City center destination entry
                } else if (i == 1 || i == targetSegmentCount - 2) {
                    speedFactors[i] = 0.90; // Peri-urban / toll approaches
                } else {
                    speedFactors[i] = 1.20; // Open highway cruising
                }
            } else {
                speedFactors[i] = 1.0;
            }
            // Inverse of speed factor gives duration weight
            double durationWeight = 1.0 / speedFactors[i];
            sumWeights += durationWeight;
        }

        double cumulativeDistance = 0.0;
        double cumulativeMinutes = 0.0;

        for (int i = 0; i < targetSegmentCount; i++) {
            int startIdx = (int) Math.round(((double) i / targetSegmentCount) * (coordCount - 1));
            int endIdx = (int) Math.round(((double) (i + 1) / targetSegmentCount) * (coordCount - 1));
            endIdx = Math.min(coordCount - 1, Math.max(startIdx, endIdx));

            List<Double> startCoord = path.get(startIdx);
            List<Double> endCoord = path.get(endIdx);

            // Coordinates for this sub-segment polyline
            List<List<Double>> subPath = new ArrayList<>();
            for (int k = startIdx; k <= endIdx; k++) {
                subPath.add(path.get(k));
            }

            double segDist = Math.round(baseSegDist * 10.0) / 10.0;
            double durationWeight = 1.0 / speedFactors[i];
            double segMinutes = Math.round(((durationWeight / sumWeights) * totalMinutes) * 10.0) / 10.0;

            cumulativeDistance += segDist;
            cumulativeMinutes += segMinutes;

            RouteSegment seg = new RouteSegment();
            seg.setSegmentIndex(i + 1);
            seg.setStartLon(startCoord.get(0));
            seg.setStartLat(startCoord.get(1));
            seg.setEndLon(endCoord.get(0));
            seg.setEndLat(endCoord.get(1));
            seg.setDistanceKm(segDist);
            seg.setEstimatedMinutes(segMinutes);
            seg.setCumulativeDistanceKm(Math.round(cumulativeDistance * 10.0) / 10.0);
            seg.setCumulativeMinutes(Math.round(cumulativeMinutes * 10.0) / 10.0);
            seg.setPath(subPath);

            segments.add(seg);
        }

        return segments;
    }
}
