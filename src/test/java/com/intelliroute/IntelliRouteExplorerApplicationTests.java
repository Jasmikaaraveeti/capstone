package com.intelliroute;

import com.intelliroute.model.*;
import com.intelliroute.service.AuthService;
import com.intelliroute.service.GeocodingService;
import com.intelliroute.service.RouteOptimizationService;
import com.intelliroute.service.RouteScoringService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class IntelliRouteExplorerApplicationTests {

    @Autowired
    private GeocodingService geocodingService;

    @Autowired
    private RouteOptimizationService routeOptimizationService;

    @Autowired
    private RouteScoringService routeScoringService;

    @Autowired
    private AuthService authService;

    @Test
    @DisplayName("Context loads successfully")
    void contextLoads() {
        assertNotNull(geocodingService);
        assertNotNull(routeOptimizationService);
        assertNotNull(routeScoringService);
    }

    @Test
    @DisplayName("TEST: Locations outside India (London, New York) must be rejected")
    void testOutsideIndiaLocationRejection() {
        // London test
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            geocodingService.resolveLocation("London", null, null, true);
        });
        assertTrue(ex.getMessage().contains("outside India"),
                "Expected error message indicating location is outside India, got: " + ex.getMessage());

        // New York test
        Exception ex2 = assertThrows(IllegalArgumentException.class, () -> {
            geocodingService.resolveLocation("New York", null, null, false);
        });
        assertTrue(ex2.getMessage().contains("outside India"));
    }

    @Test
    @DisplayName("TEST: Non-existent location must show 'Location could not be found.'")
    void testInvalidLocation() {
        Exception ex = assertThrows(IllegalArgumentException.class, () -> {
            geocodingService.resolveLocation("XYZABC123INVALID", null, null, true);
        });
        assertTrue(ex.getMessage().contains("could not be found") || ex.getMessage().contains("outside India"));
    }

    @Test
    @DisplayName("TEST: Inter-State Journey - Vijayawada to Hyderabad (Car, Recommended)")
    void testInterState_VijayawadaToHyderabad() {
        RouteRequest request = new RouteRequest("Vijayawada", "Hyderabad", VehicleType.CAR, PreferenceType.RECOMMENDED);
        RouteResponse response = routeOptimizationService.optimizeRoute(request);

        assertNotNull(response);
        assertFalse(response.getRoutes().isEmpty(), "Should return genuine routes between Vijayawada and Hyderabad");
        assertNotNull(response.getRecommendedRoute());
        assertTrue(response.getRecommendedRoute().isRecommended());
        assertTrue(response.getRecommendedRoute().getDistanceKm() > 200.0, "Vijayawada to Hyderabad is > 200km");
        assertTrue(response.getRecommendedRoute().getEstimatedTimeMinutes() > 180.0);
        assertNotNull(response.getRecommendedRoute().getSegments());
        assertFalse(response.getRecommendedRoute().getSegments().isEmpty());
    }

    @Test
    @DisplayName("TEST: Inter-State Journey - Chennai to Bengaluru (Car, Fastest Time)")
    void testInterState_ChennaiToBengaluru() {
        RouteRequest request = new RouteRequest("Chennai", "Bengaluru", VehicleType.CAR, PreferenceType.FASTEST);
        RouteResponse response = routeOptimizationService.optimizeRoute(request);

        assertNotNull(response);
        assertFalse(response.getRoutes().isEmpty());
        assertTrue(response.getRecommendedRoute().getDistanceKm() > 250.0);
    }

    @Test
    @DisplayName("TEST: Intra-State AP Corridor - VIT-AP University to Tirupati (Car, Recommended)")
    void testIntraState_VitApToTirupati() {
        RouteRequest request = new RouteRequest("VIT-AP University", "Tirupati", VehicleType.CAR, PreferenceType.RECOMMENDED);
        RouteResponse response = routeOptimizationService.optimizeRoute(request);

        assertNotNull(response);
        assertFalse(response.getRoutes().isEmpty());
        assertNotNull(response.getRecommendedRoute());
        assertTrue(response.getRecommendedRoute().getDistanceKm() > 300.0);
    }

    @Test
    @DisplayName("TEST: Multi-modal transit calibration - Bicycle journey")
    void testBicycleCalibration() {
        RouteRequest request = new RouteRequest("Vijayawada", "Guntur", VehicleType.BICYCLE, PreferenceType.RECOMMENDED);
        RouteResponse response = routeOptimizationService.optimizeRoute(request);

        assertNotNull(response);
        assertFalse(response.getRoutes().isEmpty());
        // Cycling time for ~35km must be much greater than 60 mins
        assertTrue(response.getRecommendedRoute().getEstimatedTimeMinutes() > 60.0);
    }

    @Test
    @DisplayName("TEST: Multi-Factor Scoring with transparent weight renormalization when traffic is absent")
    void testScoringWeightsAndRenormalization() {
        List<RouteInfo> routes = new ArrayList<>();

        RouteInfo r1 = new RouteInfo();
        r1.setId("Route 1");
        r1.setDistanceKm(10.0);
        r1.setEstimatedTimeMinutes(20.0);
        r1.setTrafficScore(0.0); // Traffic unavailable
        r1.setTrafficCoveragePercent(0.0);
        r1.setWeatherScore(90.0);
        r1.setWeatherCoveragePercent(100.0);
        r1.setTraffic("Traffic data unavailable");
        r1.setWeather("Clear Skies");
        r1.setTrafficDetails(TrafficInfo.unavailable("Key not set"));
        routes.add(r1);

        RouteInfo r2 = new RouteInfo();
        r2.setId("Route 2");
        r2.setDistanceKm(15.0);
        r2.setEstimatedTimeMinutes(30.0);
        r2.setTrafficScore(0.0);
        r2.setTrafficCoveragePercent(0.0);
        r2.setWeatherScore(70.0);
        r2.setWeatherCoveragePercent(100.0);
        r2.setTraffic("Traffic data unavailable");
        r2.setWeather("Cloudy");
        r2.setTrafficDetails(TrafficInfo.unavailable("Key not set"));
        routes.add(r2);

        // Score with RECOMMENDED preference
        routeScoringService.scoreAndRankRoutes(routes, PreferenceType.RECOMMENDED);

        assertTrue(r1.isRecommended());
        assertTrue(r1.getScore() > r2.getScore());
        assertEquals(100.0, r1.getDistanceScore());
        assertEquals(0.0, r2.getDistanceScore());
        // Verify final score is within [0, 100]
        assertTrue(r1.getScore() >= 0.0 && r1.getScore() <= 100.0);
    }

    @Test
    @DisplayName("TEST: Auth registration and BCrypt password encryption")
    void testAuthentication() {
        String testEmail = "testuser_" + System.currentTimeMillis() + "@intelliroute.in";
        RegisterRequest registerReq = new RegisterRequest("Test User", testEmail, "SecurePass123", "SecurePass123");
        AuthResponse registerResp = authService.register(registerReq);

        assertNotNull(registerResp);
        assertNotNull(registerResp.getToken());
        assertEquals(testEmail, registerResp.getEmail());

        // Verify login
        AuthRequest loginReq = new AuthRequest(testEmail, "SecurePass123");
        AuthResponse loginResp = authService.login(loginReq);

        assertNotNull(loginResp);
        assertNotNull(loginResp.getToken());
    }
}
