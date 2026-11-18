package com.intelliroute.controller;

import com.intelliroute.model.Coordinate;
import com.intelliroute.model.RouteRequest;
import com.intelliroute.model.RouteResponse;
import com.intelliroute.repository.UserRepository;
import com.intelliroute.service.GeocodingService;
import com.intelliroute.service.RouteOptimizationService;
import com.intelliroute.service.TrafficService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/routes")
public class RouteController {

    private final RouteOptimizationService routeOptimizationService;
    private final GeocodingService geocodingService;
    private final TrafficService trafficService;
    private final UserRepository userRepository;

    public RouteController(RouteOptimizationService routeOptimizationService,
                           GeocodingService geocodingService,
                           TrafficService trafficService,
                           UserRepository userRepository) {
        this.routeOptimizationService = routeOptimizationService;
        this.geocodingService = geocodingService;
        this.trafficService = trafficService;
        this.userRepository = userRepository;
    }

    @GetMapping("/status")
    public ResponseEntity<Map<String, Object>> getStatus() {
        Map<String, Object> status = new HashMap<>();
        status.put("service", "IntelliRoute Explorer API - India-Wide Multi-Factor Route Optimization");
        status.put("status", "UP");
        status.put("region", "India (All States & Union Territories)");

        // Dynamic database connection check
        boolean dbHealthy = false;
        try {
            userRepository.count();
            dbHealthy = true;
        } catch (Exception ignored) {
        }
        status.put("database", dbHealthy ? "Database Connected (Healthy)" : "Database Disconnected");

        status.put("routingProvider", "OSRM (Open Source Routing Machine) - GeoJSON Engine");
        status.put("geocodingProvider", "Nominatim (OpenStreetMap India)");
        status.put("weatherProvider", "Open-Meteo Hourly Forecast API (Time-Aware Telemetry)");

        boolean trafficConfigured = trafficService.isTomTomConfigured();
        status.put("trafficKeyConfigured", trafficConfigured);
        status.put("trafficProvider", trafficConfigured ?
                "TomTom Traffic Flow API (Live Absolute Telemetry)" :
                "Corridor Traffic Telemetry (OSRM Speed & Congestion Model)");

        status.put("supportedHubsCount", geocodingService.getAllLocations().size());
        status.put("timestamp", System.currentTimeMillis());

        return ResponseEntity.ok(status);
    }

    @PostMapping("/traffic-key")
    public ResponseEntity<Map<String, Object>> updateTrafficKey(@RequestBody Map<String, String> body) {
        String key = body != null ? body.get("apiKey") : null;
        trafficService.setTomtomApiKey(key != null ? key.trim() : null);

        Map<String, Object> resp = new HashMap<>();
        resp.put("success", true);
        resp.put("configured", trafficService.isTomTomConfigured());
        resp.put("message", trafficService.isTomTomConfigured() ?
                "TomTom API Key updated successfully (Live absolute telemetry active)" :
                "TomTom API Key cleared (Corridor segment traffic engine active)");
        return ResponseEntity.ok(resp);
    }

    @GetMapping("/locations")
    public ResponseEntity<List<Coordinate>> getLocations(@RequestParam(value = "q", required = false) String query) {
        List<Coordinate> locations = geocodingService.searchLocations(query);
        return ResponseEntity.ok(locations);
    }

    @PostMapping("/optimize")
    public ResponseEntity<RouteResponse> optimizeRoute(@Valid @RequestBody RouteRequest request) {
        RouteResponse response = routeOptimizationService.optimizeRoute(request);
        return ResponseEntity.ok(response);
    }
}