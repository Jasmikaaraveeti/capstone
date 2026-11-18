package com.intelliroute.model;

import java.util.List;

public class RouteInfo {
    private String id;
    private String routeType; // "Route 1", "Route 2", etc.
    private String routeName; // e.g. "Via NH16"
    private double distanceKm;
    private double estimatedTimeMinutes;
    private String traffic;
    private double trafficScore;
    private TrafficInfo trafficDetails;
    private String weather;
    private double weatherScore;
    private WeatherInfo weatherDetails;
    private double distanceScore;
    private double timeScore;
    private double score;
    private boolean recommended;
    private String recommendationReason;
    private List<List<Double>> path; // GeoJSON coordinates [[lon, lat], ...]
    private List<RouteSegment> segments; // Detailed road sections with traffic & weather
    private double trafficCoveragePercent;
    private double weatherCoveragePercent;
    private int congestedSegmentCount;
    private int rainAffectedSegmentCount;
    private String dataQuality;

    public RouteInfo() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getRouteType() {
        return routeType;
    }

    public void setRouteType(String routeType) {
        this.routeType = routeType;
    }

    public String getRouteName() {
        return routeName;
    }

    public void setRouteName(String routeName) {
        this.routeName = routeName;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getEstimatedTimeMinutes() {
        return estimatedTimeMinutes;
    }

    public void setEstimatedTimeMinutes(double estimatedTimeMinutes) {
        this.estimatedTimeMinutes = estimatedTimeMinutes;
    }

    public String getTraffic() {
        return traffic;
    }

    public void setTraffic(String traffic) {
        this.traffic = traffic;
    }

    public double getTrafficScore() {
        return trafficScore;
    }

    public void setTrafficScore(double trafficScore) {
        this.trafficScore = trafficScore;
    }

    public TrafficInfo getTrafficDetails() {
        return trafficDetails;
    }

    public void setTrafficDetails(TrafficInfo trafficDetails) {
        this.trafficDetails = trafficDetails;
    }

    public String getWeather() {
        return weather;
    }

    public void setWeather(String weather) {
        this.weather = weather;
    }

    public double getWeatherScore() {
        return weatherScore;
    }

    public void setWeatherScore(double weatherScore) {
        this.weatherScore = weatherScore;
    }

    public WeatherInfo getWeatherDetails() {
        return weatherDetails;
    }

    public void setWeatherDetails(WeatherInfo weatherDetails) {
        this.weatherDetails = weatherDetails;
    }

    public double getDistanceScore() {
        return distanceScore;
    }

    public void setDistanceScore(double distanceScore) {
        this.distanceScore = distanceScore;
    }

    public double getTimeScore() {
        return timeScore;
    }

    public void setTimeScore(double timeScore) {
        this.timeScore = timeScore;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
    }

    public boolean isRecommended() {
        return recommended;
    }

    public void setRecommended(boolean recommended) {
        this.recommended = recommended;
    }

    public String getRecommendationReason() {
        return recommendationReason;
    }

    public void setRecommendationReason(String recommendationReason) {
        this.recommendationReason = recommendationReason;
    }

    public List<List<Double>> getPath() {
        return path;
    }

    public void setPath(List<List<Double>> path) {
        this.path = path;
    }

    public List<RouteSegment> getSegments() {
        return segments;
    }

    public void setSegments(List<RouteSegment> segments) {
        this.segments = segments;
    }

    public double getTrafficCoveragePercent() {
        return trafficCoveragePercent;
    }

    public void setTrafficCoveragePercent(double trafficCoveragePercent) {
        this.trafficCoveragePercent = trafficCoveragePercent;
    }

    public double getWeatherCoveragePercent() {
        return weatherCoveragePercent;
    }

    public void setWeatherCoveragePercent(double weatherCoveragePercent) {
        this.weatherCoveragePercent = weatherCoveragePercent;
    }

    public int getCongestedSegmentCount() {
        return congestedSegmentCount;
    }

    public void setCongestedSegmentCount(int congestedSegmentCount) {
        this.congestedSegmentCount = congestedSegmentCount;
    }

    public int getRainAffectedSegmentCount() {
        return rainAffectedSegmentCount;
    }

    public void setRainAffectedSegmentCount(int rainAffectedSegmentCount) {
        this.rainAffectedSegmentCount = rainAffectedSegmentCount;
    }

    public String getDataQuality() {
        return dataQuality;
    }

    public void setDataQuality(String dataQuality) {
        this.dataQuality = dataQuality;
    }
}
