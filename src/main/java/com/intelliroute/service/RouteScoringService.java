package com.intelliroute.service;

import com.intelliroute.model.PreferenceType;
import com.intelliroute.model.RouteInfo;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;

@Service
public class RouteScoringService {

    public void scoreAndRankRoutes(List<RouteInfo> routes, PreferenceType preference) {
        if (routes == null || routes.isEmpty()) {
            return;
        }

        // 1. Find min and max for distance and time
        double minDistance = Double.MAX_VALUE;
        double maxDistance = Double.MIN_VALUE;
        double minTime = Double.MAX_VALUE;
        double maxTime = Double.MIN_VALUE;

        for (RouteInfo route : routes) {
            double dist = route.getDistanceKm();
            double time = route.getEstimatedTimeMinutes();

            if (dist < minDistance) minDistance = dist;
            if (dist > maxDistance) maxDistance = dist;

            if (time < minTime) minTime = time;
            if (time > maxTime) maxTime = time;
        }

        // 2. Base weights depending on user preference
        double baseTime;
        double baseTraffic;
        double baseDistance;
        double baseWeather;

        switch (preference) {
            case FASTEST:
                baseTime = 0.50;
                baseTraffic = 0.30;
                baseDistance = 0.15;
                baseWeather = 0.05;
                break;
            case SHORTEST:
                baseDistance = 0.50;
                baseTime = 0.25;
                baseTraffic = 0.20;
                baseWeather = 0.05;
                break;
            case WEATHER_SAFE:
                baseWeather = 0.50;
                baseTraffic = 0.25;
                baseTime = 0.15;
                baseDistance = 0.10;
                break;
            case RECOMMENDED:
            default:
                baseTraffic = 0.30;
                baseWeather = 0.25;
                baseDistance = 0.20;
                baseTime = 0.25;
                break;
        }

        // 3. Compute normalized scores and weighted final score for each route with transparent weight renormalization
        for (RouteInfo route : routes) {
            // Distance normalization (LOWER = BETTER)
            double distScore;
            if (Double.compare(maxDistance, minDistance) == 0) {
                distScore = 100.0;
            } else {
                distScore = ((maxDistance - route.getDistanceKm()) / (maxDistance - minDistance)) * 100.0;
            }
            distScore = Math.max(0.0, Math.min(100.0, distScore));
            route.setDistanceScore(Math.round(distScore * 10.0) / 10.0);

            // Time normalization (LOWER = BETTER)
            double timeScore;
            if (Double.compare(maxTime, minTime) == 0) {
                timeScore = 100.0;
            } else {
                timeScore = ((maxTime - route.getEstimatedTimeMinutes()) / (maxTime - minTime)) * 100.0;
            }
            timeScore = Math.max(0.0, Math.min(100.0, timeScore));
            route.setTimeScore(Math.round(timeScore * 10.0) / 10.0);

            // Check availability of live traffic and weather
            boolean trafficAvailable = (route.getTrafficDetails() != null && route.getTrafficDetails().isAvailable() && route.getTrafficCoveragePercent() > 0);
            boolean weatherAvailable = (route.getWeatherDetails() != null && route.getWeatherDetails().isAvailable() && route.getWeatherCoveragePercent() > 0);

            double trafScore = trafficAvailable ? route.getTrafficScore() : 0.0;
            double wthScore = weatherAvailable ? route.getWeatherScore() : 0.0;

            // Transparent weight renormalization for available factors
            double activeWeightSum = baseDistance + baseTime;
            if (trafficAvailable) activeWeightSum += baseTraffic;
            if (weatherAvailable) activeWeightSum += baseWeather;

            double effDistanceWeight = baseDistance / activeWeightSum;
            double effTimeWeight = baseTime / activeWeightSum;
            double effTrafficWeight = trafficAvailable ? (baseTraffic / activeWeightSum) : 0.0;
            double effWeatherWeight = weatherAvailable ? (baseWeather / activeWeightSum) : 0.0;

            // Calculate final weighted score
            double finalScore = (distScore * effDistanceWeight) +
                                (timeScore * effTimeWeight) +
                                (trafScore * effTrafficWeight) +
                                (wthScore * effWeatherWeight);

            finalScore = Math.max(0.0, Math.min(100.0, Math.round(finalScore * 10.0) / 10.0));
            route.setScore(finalScore);
            route.setRecommended(false);

            // Set data quality descriptor
            if (trafficAvailable && weatherAvailable) {
                String trafSource = (route.getTrafficDetails() != null && route.getTrafficDetails().getSource() != null) ?
                        route.getTrafficDetails().getSource() : "Segment Traffic";
                route.setDataQuality("Full Telemetry (" + trafSource + " & Weather Forecast)");
            } else if (weatherAvailable) {
                route.setDataQuality("Partial Telemetry (Weather & Route Geometry; Traffic Unavailable)");
            } else if (trafficAvailable) {
                route.setDataQuality("Partial Telemetry (Live Traffic & Route Geometry; Weather Unavailable)");
            } else {
                route.setDataQuality("Route Geometry Telemetry only");
            }
        }

        // 4. Sort descending by score
        routes.sort(Comparator.comparingDouble(RouteInfo::getScore).reversed());

        // 5. Flag highest scoring route as recommended and generate factual explanation
        if (!routes.isEmpty()) {
            RouteInfo winner = routes.get(0);
            winner.setRecommended(true);

            String explanation = buildFactualExplanation(winner, routes, preference);
            winner.setRecommendationReason(explanation);

            // Set clean descriptive reasons on alternatives
            for (int i = 1; i < routes.size(); i++) {
                RouteInfo alt = routes.get(i);
                alt.setRecommendationReason(buildAlternativeExplanation(alt, winner));
            }
        }
    }

    private String buildFactualExplanation(RouteInfo winner, List<RouteInfo> allRoutes, PreferenceType preference) {
        String trafDesc = winner.getTraffic() != null ? winner.getTraffic().toLowerCase() : "favorable";
        if (!trafDesc.contains("traffic") && !trafDesc.contains("congestion")) {
            trafDesc += " traffic";
        }

        if (allRoutes.size() == 1) {
            return String.format(Locale.US,
                    "Optimal direct route available: %.1f km in ~%.0f mins with %s weather and %s.",
                    winner.getDistanceKm(), winner.getEstimatedTimeMinutes(),
                    winner.getWeather().toLowerCase(), trafDesc);
        }

        RouteInfo runnerUp = allRoutes.get(1);
        double timeDiff = runnerUp.getEstimatedTimeMinutes() - winner.getEstimatedTimeMinutes();
        double distDiff = runnerUp.getDistanceKm() - winner.getDistanceKm();

        StringBuilder sb = new StringBuilder();

        switch (preference) {
            case FASTEST:
                if (timeDiff > 0) {
                    sb.append(String.format(Locale.US,
                            "Recommended because it provides the fastest transit time (~%.0f mins, saving %.1f mins compared to %s).",
                            winner.getEstimatedTimeMinutes(), timeDiff, runnerUp.getId()));
                } else {
                    sb.append(String.format(Locale.US,
                            "Recommended for fastest transit with ~%.0f minutes travel time across the corridor.",
                            winner.getEstimatedTimeMinutes()));
                }
                break;

            case SHORTEST:
                if (distDiff > 0) {
                    sb.append(String.format(Locale.US,
                            "Recommended because it is the shortest route (%.1f km, saving %.1f km compared to %s).",
                            winner.getDistanceKm(), distDiff, runnerUp.getId()));
                } else {
                    sb.append(String.format(Locale.US,
                            "Recommended for lowest travel distance (%.1f km) across the corridor.",
                            winner.getDistanceKm()));
                }
                break;

            case WEATHER_SAFE:
                sb.append(String.format(Locale.US,
                        "Recommended because it offers optimal weather safety conditions (%s, score %.1f/100) and reliable transit.",
                        winner.getWeather(), winner.getWeatherScore()));
                break;

            case RECOMMENDED:
            default:
                if (winner.getEstimatedTimeMinutes() <= runnerUp.getEstimatedTimeMinutes() &&
                    winner.getDistanceKm() <= runnerUp.getDistanceKm()) {
                    sb.append(String.format(Locale.US,
                            "Recommended because it offers both the shortest distance (%.1f km) and quickest arrival (~%.0f mins).",
                            winner.getDistanceKm(), winner.getEstimatedTimeMinutes()));
                } else if (winner.getTrafficScore() > runnerUp.getTrafficScore() && winner.getTrafficScore() > 0) {
                    sb.append(String.format(Locale.US,
                            "Recommended because it experiences significantly lighter traffic (%s) while preserving balanced transit time.",
                            winner.getTraffic()));
                } else {
                    sb.append(String.format(Locale.US,
                            "Recommended because it provides the best overall balance of travel time (%.0f mins), distance (%.1f km), traffic (%s), and weather (%s).",
                            winner.getEstimatedTimeMinutes(), winner.getDistanceKm(),
                            winner.getTraffic(), winner.getWeather()));
                }
                break;
        }

        return sb.toString();
    }

    private String buildAlternativeExplanation(RouteInfo alt, RouteInfo winner) {
        double timeDiff = alt.getEstimatedTimeMinutes() - winner.getEstimatedTimeMinutes();
        double distDiff = alt.getDistanceKm() - winner.getDistanceKm();

        if (timeDiff > 0 && distDiff > 0) {
            return String.format(Locale.US, "Alternative route: +%.1f km longer and +%.0f mins slower than recommended route.", distDiff, timeDiff);
        } else if (timeDiff > 0) {
            return String.format(Locale.US, "Alternative route: +%.0f mins slower transit time.", timeDiff);
        } else if (distDiff > 0) {
            return String.format(Locale.US, "Alternative route: +%.1f km additional distance.", distDiff);
        }
        return "Alternative feasible route across the corridor.";
    }
}
