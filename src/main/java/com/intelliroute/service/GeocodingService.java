package com.intelliroute.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.intelliroute.model.Coordinate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class GeocodingService {

    private static final Logger logger = LoggerFactory.getLogger(GeocodingService.class);

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build();

    @Value("${geocoding.nominatim.url:https://nominatim.openstreetmap.org}")
    private String nominatimBaseUrl;

    // Cache to avoid repeated Nominatim calls
    private final Map<String, Coordinate> geocodeCache = new ConcurrentHashMap<>();

    // Curated initial pan-India hub suggestions for instant autocomplete without network delays
    private final List<Coordinate> popularIndiaHubs = new ArrayList<>();

    // India Bounding Box: ~6.5°N (Indira Point/Great Nicobar) to 37.5°N (Ladakh), 68.0°E (Gujarat) to 97.5°E (Arunachal Pradesh)
    private static final double INDIA_MIN_LAT = 6.5;
    private static final double INDIA_MAX_LAT = 37.5;
    private static final double INDIA_MIN_LON = 68.0;
    private static final double INDIA_MAX_LON = 97.5;

    public GeocodingService() {
        initPopularIndiaHubs();
    }

    private void initPopularIndiaHubs() {
        addHub("Delhi", "New Delhi", "Delhi", 28.6139, 77.2090);
        addHub("Mumbai", "Mumbai City", "Maharashtra", 19.0760, 72.8777);
        addHub("Bengaluru", "Bengaluru Urban", "Karnataka", 12.9716, 77.5946);
        addHub("Hyderabad", "Hyderabad", "Telangana", 17.3850, 78.4867);
        addHub("Chennai", "Chennai", "Tamil Nadu", 13.0827, 80.2707);
        addHub("Kolkata", "Kolkata", "West Bengal", 22.5726, 88.3639);
        addHub("Pune", "Pune", "Maharashtra", 18.5204, 73.8567);
        addHub("Ahmedabad", "Ahmedabad", "Gujarat", 23.0225, 72.5714);
        addHub("Jaipur", "Jaipur", "Rajasthan", 26.9124, 75.7873);
        addHub("Kochi", "Ernakulam", "Kerala", 9.9312, 76.2673);
        addHub("Visakhapatnam", "Visakhapatnam", "Andhra Pradesh", 17.6868, 83.2185);
        addHub("Vijayawada", "NTR District", "Andhra Pradesh", 16.5062, 80.6480);
        addHub("Guntur", "Guntur", "Andhra Pradesh", 16.3067, 80.4365);
        addHub("Tirupati", "Tirupati", "Andhra Pradesh", 13.6288, 79.4192);
        addHub("VIT-AP University", "Amaravati", "Andhra Pradesh", 16.4971, 80.5005);
        addHub("Lucknow", "Lucknow", "Uttar Pradesh", 26.8467, 80.9462);
        addHub("Chandigarh", "Chandigarh", "Chandigarh", 30.7333, 76.7794);
        addHub("Bhopal", "Bhopal", "Madhya Pradesh", 23.2599, 77.4126);
        addHub("Patna", "Patna", "Bihar", 25.6127, 85.1589);
        addHub("Bhubaneswar", "Khordha", "Odisha", 20.2961, 85.8245);
        addHub("Guwahati", "Kamrup", "Assam", 26.1445, 91.7362);
        addHub("Nagpur", "Nagpur", "Maharashtra", 21.1458, 79.0882);
        addHub("Indore", "Indore", "Madhya Pradesh", 22.7196, 75.8577);
        addHub("Coimbatore", "Coimbatore", "Tamil Nadu", 11.0168, 76.9558);
        addHub("Varanasi", "Varanasi", "Uttar Pradesh", 25.3176, 82.9739);
        addHub("Surat", "Surat", "Gujarat", 21.1702, 72.8311);
    }

    private void addHub(String name, String district, String state, double lat, double lon) {
        Coordinate coord = new Coordinate(name, district, state, lat, lon);
        popularIndiaHubs.add(coord);
        geocodeCache.put(name.toLowerCase().trim(), coord);
    }

    public List<Coordinate> getAllLocations() {
        return Collections.unmodifiableList(popularIndiaHubs);
    }

    public List<Coordinate> searchLocations(String query) {
        if (query == null || query.isBlank()) {
            return popularIndiaHubs;
        }
        String clean = query.trim().toLowerCase();
        List<Coordinate> results = new ArrayList<>();
        for (Coordinate loc : popularIndiaHubs) {
            if (loc.getName().toLowerCase().contains(clean) ||
                (loc.getDistrict() != null && loc.getDistrict().toLowerCase().contains(clean)) ||
                (loc.getState() != null && loc.getState().toLowerCase().contains(clean))) {
                results.add(loc);
            }
        }
        return results;
    }

    /**
     * Resolve a user-entered location anywhere in India.
     */
    public Coordinate resolveLocation(String locationName, Double inputLat, Double inputLon, boolean isSource) {
        String role = isSource ? "Source" : "Destination";
        if (locationName == null || locationName.trim().isBlank()) {
            throw new IllegalArgumentException(role + " location cannot be empty.");
        }

        String trimmed = locationName.trim();
        String lower = trimmed.toLowerCase();

        // 1. Direct match in local cache
        if (geocodeCache.containsKey(lower)) {
            Coordinate cached = geocodeCache.get(lower);
            validateCoordinatesInIndia(cached.getLatitude(), cached.getLongitude(), isSource);
            return cached;
        }

        // 2. If explicit coordinates provided, validate them directly
        if (inputLat != null && inputLon != null) {
            validateCoordinatesInIndia(inputLat, inputLon, isSource);
            Coordinate coord = new Coordinate(trimmed, null, inputLat, inputLon);
            geocodeCache.put(lower, coord);
            return coord;
        }

        // 3. Query Nominatim for India-wide geocoding
        Coordinate geocoded = queryNominatim(trimmed, isSource);
        validateCoordinatesInIndia(geocoded.getLatitude(), geocoded.getLongitude(), isSource);
        geocodeCache.put(lower, geocoded);
        return geocoded;
    }

    /**
     * Query Nominatim with proper URL encoding and country restriction to India.
     * If not found within India, inspects globally to detect if location is outside India.
     */
    public Coordinate queryNominatim(String query, boolean isSource) {
        String role = isSource ? "Source" : "Destination";
        String encoded = URLEncoder.encode(query, StandardCharsets.UTF_8);

        // 1. Query globally first to accurately detect location country and prominence
        String urlGlobal = nominatimBaseUrl + "/search?q=" + encoded + "&format=json&limit=1&addressdetails=1";
        try {
            HttpRequest globalReq = HttpRequest.newBuilder()
                    .uri(URI.create(urlGlobal))
                    .header("User-Agent", "IntelliRouteExplorer/2.0 (IndiaRouteOptimization)")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> globalResp = httpClient.send(globalReq, HttpResponse.BodyHandlers.ofString());
            if (globalResp.statusCode() == 200 && globalResp.body() != null && !globalResp.body().isBlank()) {
                JsonNode root = objectMapper.readTree(globalResp.body());
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode first = root.get(0);
                    String countryCode = first.path("address").path("country_code").asText("");
                    double lat = Double.parseDouble(first.path("lat").asText("0"));
                    double lon = Double.parseDouble(first.path("lon").asText("0"));

                    if (!"in".equalsIgnoreCase(countryCode) || !isInsideIndia(lat, lon)) {
                        throw new IllegalArgumentException(role + " location is outside India.");
                    }

                    // Inside India
                    String displayName = first.path("display_name").asText(query);
                    JsonNode address = first.path("address");
                    String district = address.has("county") ? address.get("county").asText() :
                            address.has("state_district") ? address.get("state_district").asText() :
                            address.has("city") ? address.get("city").asText() : null;
                    String state = address.has("state") ? address.get("state").asText() : null;

                    Coordinate coord = new Coordinate(query, district, state, lat, lon);
                    coord.setDisplayName(displayName);
                    return coord;
                }
            }
        } catch (IllegalArgumentException e) {
            throw e;
        } catch (Exception e) {
            logger.warn("Nominatim global check exception for '{}': {}", query, e.getMessage());
        }

        // 2. Fallback query specifically with countrycodes=in for localized Indian landmarks
        String urlIn = nominatimBaseUrl + "/search?q=" + encoded + "&format=json&limit=1&countrycodes=in&addressdetails=1";
        try {
            HttpRequest request = HttpRequest.newBuilder()
                    .uri(URI.create(urlIn))
                    .header("User-Agent", "IntelliRouteExplorer/2.0 (IndiaRouteOptimization)")
                    .timeout(Duration.ofSeconds(10))
                    .GET()
                    .build();

            HttpResponse<String> response = httpClient.send(request, HttpResponse.BodyHandlers.ofString());

            if (response.statusCode() == 200 && response.body() != null && !response.body().isBlank()) {
                JsonNode root = objectMapper.readTree(response.body());
                if (root.isArray() && !root.isEmpty()) {
                    JsonNode first = root.get(0);
                    double lat = Double.parseDouble(first.path("lat").asText("0"));
                    double lon = Double.parseDouble(first.path("lon").asText("0"));

                    if (isInsideIndia(lat, lon)) {
                        String displayName = first.path("display_name").asText(query);
                        JsonNode address = first.path("address");
                        String district = address.has("county") ? address.get("county").asText() :
                                address.has("state_district") ? address.get("state_district").asText() :
                                address.has("city") ? address.get("city").asText() : null;
                        String state = address.has("state") ? address.get("state").asText() : null;

                        Coordinate coord = new Coordinate(query, district, state, lat, lon);
                        coord.setDisplayName(displayName);
                        return coord;
                    }
                }
            }
        } catch (Exception e) {
            logger.warn("Nominatim India query exception for '{}': {}", query, e.getMessage());
        }

        throw new IllegalArgumentException("Location could not be found.");
    }

    public void validateCoordinatesInIndia(double lat, double lon, boolean isSource) {
        if (!isInsideIndia(lat, lon)) {
            throw new IllegalArgumentException((isSource ? "Source" : "Destination") + " location is outside India.");
        }
    }

    public boolean isInsideIndia(double lat, double lon) {
        return lat >= INDIA_MIN_LAT && lat <= INDIA_MAX_LAT &&
               lon >= INDIA_MIN_LON && lon <= INDIA_MAX_LON;
    }
}
