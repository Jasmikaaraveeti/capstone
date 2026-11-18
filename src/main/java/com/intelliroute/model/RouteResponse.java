package com.intelliroute.model;

import java.util.List;

public class RouteResponse {
    private Coordinate source;
    private Coordinate destination;
    private VehicleType vehicle;
    private PreferenceType preference;
    private int totalRoutesFound;
    private List<RouteInfo> routes;
    private RouteInfo recommendedRoute;
    private String summary;
    private long executionTimeMs;

    public RouteResponse() {
    }

    public Coordinate getSource() {
        return source;
    }

    public void setSource(Coordinate source) {
        this.source = source;
    }

    public Coordinate getDestination() {
        return destination;
    }

    public void setDestination(Coordinate destination) {
        this.destination = destination;
    }

    public VehicleType getVehicle() {
        return vehicle;
    }

    public void setVehicle(VehicleType vehicle) {
        this.vehicle = vehicle;
    }

    public PreferenceType getPreference() {
        return preference;
    }

    public void setPreference(PreferenceType preference) {
        this.preference = preference;
    }

    public int getTotalRoutesFound() {
        return totalRoutesFound;
    }

    public void setTotalRoutesFound(int totalRoutesFound) {
        this.totalRoutesFound = totalRoutesFound;
    }

    public List<RouteInfo> getRoutes() {
        return routes;
    }

    public void setRoutes(List<RouteInfo> routes) {
        this.routes = routes;
    }

    public RouteInfo getRecommendedRoute() {
        return recommendedRoute;
    }

    public void setRecommendedRoute(RouteInfo recommendedRoute) {
        this.recommendedRoute = recommendedRoute;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public long getExecutionTimeMs() {
        return executionTimeMs;
    }

    public void setExecutionTimeMs(long executionTimeMs) {
        this.executionTimeMs = executionTimeMs;
    }
}
