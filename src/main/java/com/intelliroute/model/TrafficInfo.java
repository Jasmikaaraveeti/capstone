package com.intelliroute.model;

public class TrafficInfo {
    private String condition;
    private double score;
    private Double currentSpeedKmh;
    private Double freeFlowSpeedKmh;
    private Double delayMinutes;
    private String source;
    private boolean available;
    private int congestedSegmentCount;
    private int totalSampledSegments;
    private double coveragePercent;

    public TrafficInfo() {
    }

    public TrafficInfo(String condition, double score, Double currentSpeedKmh, Double freeFlowSpeedKmh, Double delayMinutes, String source, boolean available) {
        this.condition = condition;
        this.score = score;
        this.currentSpeedKmh = currentSpeedKmh;
        this.freeFlowSpeedKmh = freeFlowSpeedKmh;
        this.delayMinutes = delayMinutes;
        this.source = source;
        this.available = available;
        this.coveragePercent = available ? 100.0 : 0.0;
    }

    public TrafficInfo(String condition, double score, Double currentSpeedKmh, Double freeFlowSpeedKmh, Double delayMinutes, String source, boolean available, int congestedSegmentCount, int totalSampledSegments, double coveragePercent) {
        this.condition = condition;
        this.score = score;
        this.currentSpeedKmh = currentSpeedKmh;
        this.freeFlowSpeedKmh = freeFlowSpeedKmh;
        this.delayMinutes = delayMinutes;
        this.source = source;
        this.available = available;
        this.congestedSegmentCount = congestedSegmentCount;
        this.totalSampledSegments = totalSampledSegments;
        this.coveragePercent = coveragePercent;
    }

    public static TrafficInfo unavailable(String reason) {
        return new TrafficInfo("Traffic data unavailable", 0.0, null, null, null, reason != null ? reason : "Traffic data unavailable", false, 0, 0, 0.0);
    }

    public String getCondition() {
        return condition;
    }

    public void setCondition(String condition) {
        this.condition = condition;
    }

    public double getScore() {
        return score;
    }

    public void setScore(double score) {
        this.score = score;
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

    public Double getDelayMinutes() {
        return delayMinutes;
    }

    public void setDelayMinutes(Double delayMinutes) {
        this.delayMinutes = delayMinutes;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public int getCongestedSegmentCount() {
        return congestedSegmentCount;
    }

    public void setCongestedSegmentCount(int congestedSegmentCount) {
        this.congestedSegmentCount = congestedSegmentCount;
    }

    public int getTotalSampledSegments() {
        return totalSampledSegments;
    }

    public void setTotalSampledSegments(int totalSampledSegments) {
        this.totalSampledSegments = totalSampledSegments;
    }

    public double getCoveragePercent() {
        return coveragePercent;
    }

    public void setCoveragePercent(double coveragePercent) {
        this.coveragePercent = coveragePercent;
    }
}
