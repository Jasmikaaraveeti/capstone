package com.intelliroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliroute.model.RouteSegment;
import com.intelliroute.model.WeatherInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;

@Service
public class WeatherService {

    private static final Logger logger = LoggerFactory.getLogger(WeatherService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(6))
            .build();

    private final ExecutorService weatherExecutor = Executors.newFixedThreadPool(4);

    @Value("${weather.openmeteo.url:https://api.open-meteo.com/v1/forecast}")
    private String openMeteoBaseUrl;

    private static final ZoneId INDIA_ZONE = ZoneId.of("Asia/Kolkata");
    private static final DateTimeFormatter HOURLY_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:00");
    private static final DateTimeFormatter DISPLAY_TIME_FMT = DateTimeFormatter.ofPattern("hh:mm a, dd MMM");

    // In-memory cache for Open-Meteo hourly responses (30-minute TTL)
    private static class CachedForecast {
        final JsonNode hourlyNode;
        final long timestamp;

        CachedForecast(JsonNode hourlyNode) {
            this.hourlyNode = hourlyNode;
            this.timestamp = System.currentTimeMillis();
        }

        boolean isExpired() {
            return (System.currentTimeMillis() - timestamp) > 1_800_000L; // 30 minutes
        }
    }

    private final Map<String, CachedForecast> weatherCache = new ConcurrentHashMap<>();

    /**
     * Analyze time-aware weather across all sampled segments along a candidate route.
     */
    public WeatherInfo analyzeRouteSegments(List<RouteSegment> segments) {
        if (segments == null || segments.isEmpty()) {
            return WeatherInfo.unavailable("Weather data unavailable (No route segments)");
        }

        ZonedDateTime journeyStartTime = ZonedDateTime.now(INDIA_ZONE);

        List<CompletableFuture<Void>> futures = new ArrayList<>();

        for (RouteSegment segment : segments) {
            // Calculate expected arrival time at this segment based on cumulative minutes
            ZonedDateTime arrivalTime = journeyStartTime.plusMinutes((long) segment.getCumulativeMinutes());
            segment.setExpectedArrivalTime(arrivalTime.format(DISPLAY_TIME_FMT));

            double sampleLat = (segment.getStartLat() + segment.getEndLat()) / 2.0;
            double sampleLon = (segment.getStartLon() + segment.getEndLon()) / 2.0;

            futures.add(CompletableFuture.runAsync(() -> {
                querySegmentWeather(segment, sampleLat, sampleLon, arrivalTime);
            }, weatherExecutor));
        }

        try {
            CompletableFuture.allOf(futures.toArray(new CompletableFuture[0]))
                    .get(8, TimeUnit.SECONDS);
        } catch (Exception e) {
            logger.warn("Parallel segment weather querying finished with timeout/interrupt: {}", e.getMessage());
        }

        // Aggregate segment results
        int totalSegments = segments.size();
        int successfulSamples = 0;
        int rainAffectedSegments = 0;
        int severeWeatherSegments = 0;
        double totalWeightedScore = 0.0;
        double totalDistance = 0.0;
        double minTemp = Double.MAX_VALUE;
        double maxTemp = Double.MIN_VALUE;
        int maxPrecipProb = 0;
        String predominantCondition = "Clear Skies";

        for (RouteSegment segment : segments) {
            double segDist = Math.max(0.1, segment.getDistanceKm());
            totalDistance += segDist;

            if (segment.isWeatherAvailable() && segment.getTemperatureCelsius() != null) {
                successfulSamples++;
                totalWeightedScore += segment.getWeatherScore() * segDist;

                double t = segment.getTemperatureCelsius();
                if (t < minTemp) minTemp = t;
                if (t > maxTemp) maxTemp = t;

                if (segment.getPrecipitationProbability() != null && segment.getPrecipitationProbability() > maxPrecipProb) {
                    maxPrecipProb = segment.getPrecipitationProbability();
                }

                boolean isRain = (segment.getRainMm() != null && segment.getRainMm() > 0.1) ||
                                 (segment.getPrecipitationMm() != null && segment.getPrecipitationMm() > 0.1) ||
                                 (segment.getPrecipitationProbability() != null && segment.getPrecipitationProbability() >= 50);
                if (isRain) {
                    rainAffectedSegments++;
                }

                if (segment.getWeatherCode() != null && (segment.getWeatherCode() >= 95 || (segment.getRainMm() != null && segment.getRainMm() >= 4.0))) {
                    severeWeatherSegments++;
                }

                if (segment.getWeatherScore() < 60.0) {
                    predominantCondition = segment.getWeatherCondition();
                }
            }
        }

        if (successfulSamples == 0) {
            return WeatherInfo.unavailable("Weather data unavailable from Open-Meteo API");
        }

        double coveragePercent = Math.round(((double) successfulSamples / totalSegments) * 1000.0) / 10.0;
        double overallScore = Math.round((totalWeightedScore / totalDistance) * 10.0) / 10.0;

        String summary;
        if (severeWeatherSegments > 0) {
            summary = String.format(Locale.US, "%d segment(s) impacted by severe atmospheric disturbances (temp %.1f°C - %.1f°C).",
                    severeWeatherSegments, minTemp, maxTemp);
        } else if (rainAffectedSegments > 0) {
            summary = String.format(Locale.US, "%d segment(s) with rain/precipitation chances up to %d%% (temp %.1f°C - %.1f°C).",
                    rainAffectedSegments, maxPrecipProb, minTemp, maxTemp);
        } else {
            summary = String.format(Locale.US, "Favorable clear/mild transit conditions across corridor (temp %.1f°C - %.1f°C).",
                    minTemp, maxTemp);
        }

        WeatherInfo info = new WeatherInfo(
                predominantCondition,
                overallScore,
                Math.round(((minTemp + maxTemp) / 2.0) * 10.0) / 10.0,
                0.0,
                0.0,
                0.0,
                0,
                true,
                summary
        );
        info.setRainAffectedSegmentCount(rainAffectedSegments);
        info.setSevereWeatherSegmentCount(severeWeatherSegments);
        info.setTotalSampledSegments(totalSegments);
        info.setCoveragePercent(coveragePercent);
        info.setTemperatureMin(Math.round(minTemp * 10.0) / 10.0);
        info.setTemperatureMax(Math.round(maxTemp * 10.0) / 10.0);
        info.setPrecipitationProbabilityMax(maxPrecipProb);

        return info;
    }

    private void querySegmentWeather(RouteSegment segment, double lat, double lon, ZonedDateTime arrivalTime) {
        String cacheKey = String.format(Locale.US, "%.2f,%.2f", lat, lon);
        CachedForecast cached = weatherCache.get(cacheKey);
        JsonNode hourlyNode = (cached != null && !cached.isExpired()) ? cached.hourlyNode : null;

        if (hourlyNode == null) {
            try {
                String url = String.format(Locale.US,
                        "%s?latitude=%.4f&longitude=%.4f&hourly=temperature_2m,precipitation_probability,precipitation,rain,weather_code,wind_speed_10m&forecast_days=2&timezone=Asia%%2FKolkata",
                        openMeteoBaseUrl, lat, lon);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(url))
                        .header("User-Agent", "IntelliRouteExplorer/2.0")
                        .timeout(Duration.ofSeconds(6))
                        .GET()
                        .build();

                HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

                if (response.statusCode() == 200 && response.body() != null) {
                    JsonNode root = objectMapper.readTree(response.body());
                    hourlyNode = root.path("hourly");
                    if (!hourlyNode.isMissingNode()) {
                        weatherCache.put(cacheKey, new CachedForecast(hourlyNode));
                    }
                }
            } catch (Exception e) {
                logger.debug("Open-Meteo query failed for point ({}, {}): {}", lat, lon, e.getMessage());
            }
        }

        if (hourlyNode == null || hourlyNode.isMissingNode()) {
            segment.setWeatherAvailable(false);
            segment.setWeatherCondition("Unavailable");
            segment.setWeatherScore(0.0);
            return;
        }

        // Find hourly forecast index matching the segment's expected arrival time
        String targetTimeHour = arrivalTime.format(HOURLY_FMT);
        JsonNode times = hourlyNode.path("time");
        int targetIndex = 0;

        if (times.isArray() && !times.isEmpty()) {
            for (int i = 0; i < times.size(); i++) {
                if (times.get(i).asText("").equalsIgnoreCase(targetTimeHour)) {
                    targetIndex = i;
                    break;
                }
            }
        }

        double temp = getNodeValue(hourlyNode.path("temperature_2m"), targetIndex, 28.0);
        int precipProb = (int) getNodeValue(hourlyNode.path("precipitation_probability"), targetIndex, 0.0);
        double precip = getNodeValue(hourlyNode.path("precipitation"), targetIndex, 0.0);
        double rain = getNodeValue(hourlyNode.path("rain"), targetIndex, 0.0);
        int code = (int) getNodeValue(hourlyNode.path("weather_code"), targetIndex, 0.0);
        double windSpeed = getNodeValue(hourlyNode.path("wind_speed_10m"), targetIndex, 10.0);

        segment.setWeatherAvailable(true);
        segment.setTemperatureCelsius(Math.round(temp * 10.0) / 10.0);
        segment.setPrecipitationProbability(precipProb);
        segment.setPrecipitationMm(Math.round(precip * 10.0) / 10.0);
        segment.setRainMm(Math.round(rain * 10.0) / 10.0);
        segment.setWeatherCode(code);
        segment.setWindSpeedKmh(Math.round(windSpeed * 10.0) / 10.0);

        classifySegmentWeather(segment, temp, precipProb, rain, windSpeed, code);
    }

    private double getNodeValue(JsonNode arrayNode, int index, double defaultVal) {
        if (arrayNode.isArray() && index >= 0 && index < arrayNode.size()) {
            return arrayNode.get(index).asDouble(defaultVal);
        }
        return defaultVal;
    }

    private void classifySegmentWeather(RouteSegment seg, double temp, int precipProb, double rain, double windSpeed, int code) {
        String condition;
        double score;
        String summary;

        if (code >= 95) {
            condition = "Thunderstorm";
            score = 15.0;
            summary = String.format(Locale.US, "Severe thunderstorm active (%.1f°C, wind %.1f km/h)", temp, windSpeed);
        } else if (rain >= 4.0 || code == 65 || code == 82) {
            condition = "Heavy Rain";
            score = 30.0;
            summary = String.format(Locale.US, "Heavy rain %.1f mm (prob %d%%, temp %.1f°C)", rain, precipProb, temp);
        } else if (windSpeed >= 45.0) {
            condition = "High Wind";
            score = 45.0;
            summary = String.format(Locale.US, "Strong winds %.1f km/h (%.1f°C)", windSpeed, temp);
        } else if (rain > 0.1 || precipProb >= 50 || (code >= 51 && code <= 63) || code == 80) {
            condition = "Light Rain";
            score = 65.0;
            summary = String.format(Locale.US, "Rain/showers (prob %d%%, temp %.1f°C)", precipProb, temp);
        } else if (code == 2 || code == 3) {
            condition = "Cloudy";
            score = 85.0;
            summary = String.format(Locale.US, "Overcast (%.1f°C, wind %.1f km/h)", temp, windSpeed);
        } else {
            condition = "Clear Skies";
            score = 100.0;
            summary = String.format(Locale.US, "Clear skies (%.1f°C, wind %.1f km/h)", temp, windSpeed);
        }

        seg.setWeatherCondition(condition);
        seg.setWeatherScore(score);
        seg.setWeatherSummary(summary);
    }
}
