package com.intelliroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliroute.model.Coordinate;
import com.intelliroute.model.RouteInfo;
import com.intelliroute.model.VehicleType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
public class RoutingService {

    private static final Logger logger = LoggerFactory.getLogger(RoutingService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${routing.osrm.url:https://router.project-osrm.org}")
    private String osrmBaseUrl;

    public List<RouteInfo> calculateRoutes(Coordinate source, Coordinate destination, VehicleType vehicle) {
        String profile = (vehicle == VehicleType.BICYCLE) ? "bicycle" : "driving";

        // OSRM coordinates format: {lon1},{lat1};{lon2},{lat2}
        String coordsParam = String.format(Locale.US, "%.6f,%.6f;%.6f,%.6f",
                source.getLongitude(), source.getLatitude(),
                destination.getLongitude(), destination.getLatitude());

        String url = String.format("%s/route/v1/%s/%s?alternatives=true&overview=full&geometries=geojson",
                osrmBaseUrl, profile, coordsParam);

        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(url))
                    .header("User-Agent", "IntelliRouteExplorer/2.0 (IndiaRouteOptimization)")
                    .timeout(Duration.ofSeconds(25)) // Accommodates long-distance journeys across India
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() != 200 || response.body() == null || response.body().isBlank()) {
                throw new IllegalStateException("Routing service unavailable. HTTP " + response.statusCode());
            }

            JsonNode root = objectMapper.readTree(response.body());
            String code = root.path("code").asText("");
            if (!"Ok".equalsIgnoreCase(code)) {
                throw new IllegalStateException("Routing service could not find route: " + code);
            }

            JsonNode routesArray = root.path("routes");
            if (!routesArray.isArray() || routesArray.isEmpty()) {
                throw new IllegalStateException("No routes found between " + source.getName() + " and " + destination.getName() + ".");
            }

            List<RouteInfo> routeList = new ArrayList<>();
            int maxCandidates = Math.min(routesArray.size(), 3);

            for (int i = 0; i < maxCandidates; i++) {
                JsonNode routeNode = routesArray.get(i);

                // Distance in meters converted to kilometers
                double distanceMeters = routeNode.path("distance").asDouble(0.0);
                double distanceKm = Math.round((distanceMeters / 1000.0) * 10.0) / 10.0;

                // Duration in seconds converted to minutes
                double durationSec = routeNode.path("duration").asDouble(0.0);
                double rawTimeMinutes = durationSec / 60.0;

                // Calibrated vehicle transit time
                double adjustedTimeMinutes = adjustTravelTimeForVehicle(rawTimeMinutes, distanceKm, vehicle);

                // Extract GeoJSON geometry coordinates [[lon, lat], ...]
                JsonNode coordinatesNode = routeNode.path("geometry").path("coordinates");
                List<List<Double>> path = new ArrayList<>();
                if (coordinatesNode.isArray()) {
                    for (JsonNode pt : coordinatesNode) {
                        if (pt.isArray() && pt.size() >= 2) {
                            List<Double> coord = new ArrayList<>();
                            coord.add(pt.get(0).asDouble()); // lon
                            coord.add(pt.get(1).asDouble()); // lat
                            path.add(coord);
                        }
                    }
                }

                // Deduplication check: Avoid adding duplicate routes
                boolean isDuplicate = false;
                for (RouteInfo existing : routeList) {
                    if (Math.abs(existing.getDistanceKm() - distanceKm) < 0.2 &&
                        Math.abs(existing.getEstimatedTimeMinutes() - adjustedTimeMinutes) < 0.5) {
                        isDuplicate = true;
                        break;
                    }
                }
                if (isDuplicate) {
                    continue;
                }

                RouteInfo info = new RouteInfo();
                int routeNum = routeList.size() + 1;
                info.setId("Route " + routeNum);
                info.setRouteType("Route " + routeNum);

                // Extract summary or road corridor name from legs
                String summary = "";
                JsonNode legs = routeNode.path("legs");
                if (legs.isArray() && !legs.isEmpty()) {
                    summary = legs.get(0).path("summary").asText("");
                }
                if (summary == null || summary.isBlank()) {
                    summary = (routeNum == 1) ? "Primary Highway Corridor" : "Alternative Highway " + routeNum;
                }
                info.setRouteName("Via " + summary);
                info.setDistanceKm(distanceKm);
                info.setEstimatedTimeMinutes(Math.round(adjustedTimeMinutes * 10.0) / 10.0);
                info.setPath(path);

                routeList.add(info);
            }

            if (routeList.isEmpty()) {
                throw new IllegalStateException("No viable routes found between selected locations.");
            }

            return routeList;

        } catch (HttpTimeoutException e) {
            logger.error("OSRM routing request timed out for corridor: {} -> {}", source.getName(), destination.getName());
            throw new IllegalStateException("Routing request timed out. Please try a shorter distance or verify network availability.");
        } catch (IllegalStateException e) {
            throw e;
        } catch (Exception e) {
            logger.error("Routing calculation failed: {}", e.getMessage());
            throw new IllegalStateException("Routing service unavailable: " + e.getMessage());
        }
    }

    private double adjustTravelTimeForVehicle(double baseMinutes, double distanceKm, VehicleType vehicle) {
        switch (vehicle) {
            case CAR:
                return baseMinutes;
            case BUS:
                // Governed commercial bus speed limit (60-70 km/h) + terminal stops (~1.25x)
                return baseMinutes * 1.25;
            case AUTO:
                // Auto-rickshaw cruising speeds (~40-45 km/h, ~1.35x)
                return baseMinutes * 1.35;
            case BICYCLE:
                // Average cycling speed in mixed conditions (~15 km/h, 4.0 min/km)
                return distanceKm * 4.0;
            default:
                return baseMinutes;
        }
    }
}
