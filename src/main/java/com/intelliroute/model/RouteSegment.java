package com.intelliroute.model;

import java.util.List;

public class RouteSegment {
    private int segmentIndex;
    private double startLat;
    private double startLon;
    private double endLat;
    private double endLon;
    private double distanceKm;
    private double estimatedMinutes;
    private double cumulativeDistanceKm;
    private double cumulativeMinutes;
    private String expectedArrivalTime; // Formatted arrival time, e.g. "14:30" or ISO

    // Segment Coordinates for GeoJSON Polyline slice
    private List<List<Double>> path; // [[lon, lat], ...]

    // TomTom Traffic Flow Info
    private boolean trafficAvailable;
    private Double currentSpeedKmh;
    private Double freeFlowSpeedKmh;
    private Double currentTravelTimeSec;
    private Double freeFlowTravelTimeSec;
    private Double confidence;
    private Double congestionRatio;
    private String congestionLevel; // "FREE_FLOW", "MODERATE", "HEAVY", "SEVERE", "UNAVAILABLE"
    private double trafficScore;

    // Open-Meteo Weather Info (Time-Aware)
    private boolean weatherAvailable;
    private Double temperatureCelsius;
    private Double precipitationMm;
    private Double rainMm;
    private Integer precipitationProbability;
    private Double windSpeedKmh;
    private Integer weatherCode;
    private String weatherCondition;
    private String weatherSummary;
    private double weatherScore;

    public RouteSegment() {
    }

    public int getSegmentIndex() {
        return segmentIndex;
    }

    public void setSegmentIndex(int segmentIndex) {
        this.segmentIndex = segmentIndex;
    }

    public double getStartLat() {
        return startLat;
    }

    public void setStartLat(double startLat) {
        this.startLat = startLat;
    }

    public double getStartLon() {
        return startLon;
    }

    public void setStartLon(double startLon) {
        this.startLon = startLon;
    }

    public double getEndLat() {
        return endLat;
    }

    public void setEndLat(double endLat) {
        this.endLat = endLat;
    }

    public double getEndLon() {
        return endLon;
    }

    public void setEndLon(double endLon) {
        this.endLon = endLon;
    }

    public double getDistanceKm() {
        return distanceKm;
    }

    public void setDistanceKm(double distanceKm) {
        this.distanceKm = distanceKm;
    }

    public double getEstimatedMinutes() {
        return estimatedMinutes;
    }

    public void setEstimatedMinutes(double estimatedMinutes) {
        this.estimatedMinutes = estimatedMinutes;
    }

    public double getCumulativeDistanceKm() {
        return cumulativeDistanceKm;
    }

    public void setCumulativeDistanceKm(double cumulativeDistanceKm) {
        this.cumulativeDistanceKm = cumulativeDistanceKm;
    }

    public double getCumulativeMinutes() {
        return cumulativeMinutes;
    }

    public void setCumulativeMinutes(double cumulativeMinutes) {
        this.cumulativeMinutes = cumulativeMinutes;
    }

    public String getExpectedArrivalTime() {
        return expectedArrivalTime;
    }

    public void setExpectedArrivalTime(String expectedArrivalTime) {
        this.expectedArrivalTime = expectedArrivalTime;
    }

    public List<List<Double>> getPath() {
        return path;
    }

    public void setPath(List<List<Double>> path) {
        this.path = path;
    }

    public boolean isTrafficAvailable() {
        return trafficAvailable;
    }

    public void setTrafficAvailable(boolean trafficAvailable) {
        this.trafficAvailable = trafficAvailable;
    }

    public Double getCurrentSpeedKmh() {
        return currentSpeedKmh;
    }

    public void setCurrentSpeedKmh(Double currentSpeedKmh) {
        this.currentSpeedKmh = currentSpeedKmh;
    }

    public Double getFreeFlowSpeedKmh() {
        return freeFlowSpeedKmh;
    }

    public void setFreeFlowSpeedKmh(Double freeFlowSpeedKmh) {
        this.freeFlowSpeedKmh = freeFlowSpeedKmh;
    }

    public Double getCurrentTravelTimeSec() {
        return currentTravelTimeSec;
    }

    public void setCurrentTravelTimeSec(Double currentTravelTimeSec) {
        this.currentTravelTimeSec = currentTravelTimeSec;
    }

    public Double getFreeFlowTravelTimeSec() {
        return freeFlowTravelTimeSec;
    }

    public void setFreeFlowTravelTimeSec(Double freeFlowTravelTimeSec) {
        this.freeFlowTravelTimeSec = freeFlowTravelTimeSec;
    }

    public Double getConfidence() {
        return confidence;
    }

    public void setConfidence(Double confidence) {
        this.confidence = confidence;
    }

    public Double getCongestionRatio() {
        return congestionRatio;
    }

    public void setCongestionRatio(Double congestionRatio) {
        this.congestionRatio = congestionRatio;
    }

    public String getCongestionLevel() {
        return congestionLevel;
    }

    public void setCongestionLevel(String congestionLevel) {
        this.congestionLevel = congestionLevel;
    }

    public double getTrafficScore() {
        return trafficScore;
    }

    public void setTrafficScore(double trafficScore) {
        this.trafficScore = trafficScore;
    }

    public boolean isWeatherAvailable() {
        return weatherAvailable;
    }

    public void setWeatherAvailable(boolean weatherAvailable) {
        this.weatherAvailable = weatherAvailable;
    }

    public Double getTemperatureCelsius() {
        return temperatureCelsius;
    }

    public void setTemperatureCelsius(Double temperatureCelsius) {
        this.temperatureCelsius = temperatureCelsius;
    }

    public Double getPrecipitationMm() {
        return precipitationMm;
    }

    public void setPrecipitationMm(Double precipitationMm) {
        this.precipitationMm = precipitationMm;
    }

    public Double getRainMm() {
        return rainMm;
    }

    public void setRainMm(Double rainMm) {
        this.rainMm = rainMm;
    }

    public Integer getPrecipitationProbability() {
        return precipitationProbability;
    }

    public void setPrecipitationProbability(Integer precipitationProbability) {
        this.precipitationProbability = precipitationProbability;
    }

    public Double getWindSpeedKmh() {
        return windSpeedKmh;
    }

    public void setWindSpeedKmh(Double windSpeedKmh) {
        this.windSpeedKmh = windSpeedKmh;
    }

    public Integer getWeatherCode() {
        return weatherCode;
    }

    public void setWeatherCode(Integer weatherCode) {
        this.weatherCode = weatherCode;
    }

    public String getWeatherCondition() {
        return weatherCondition;
    }

    public void setWeatherCondition(String weatherCondition) {
        this.weatherCondition = weatherCondition;
    }

    public String getWeatherSummary() {
        return weatherSummary;
    }

    public void setWeatherSummary(String weatherSummary) {
        this.weatherSummary = weatherSummary;
    }

    public double getWeatherScore() {
        return weatherScore;
    }

    public void setWeatherScore(double weatherScore) {
        this.weatherScore = weatherScore;
    }
}
