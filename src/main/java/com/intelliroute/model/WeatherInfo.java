package com.intelliroute.model;

public class WeatherInfo {
    private String condition;
    private double score;
    private Double temperatureCelsius;
    private Double precipitationMm;
    private Double rainMm;
    private Double windSpeedKmh;
    private Integer weatherCode;
    private boolean available;
    private String summary;
    private int rainAffectedSegmentCount;
    private int severeWeatherSegmentCount;
    private int totalSampledSegments;
    private double coveragePercent;
    private Double temperatureMin;
    private Double temperatureMax;
    private Integer precipitationProbabilityMax;

    public WeatherInfo() {
    }

    public WeatherInfo(String condition, double score, Double temperatureCelsius, Double precipitationMm, Double rainMm, Double windSpeedKmh, Integer weatherCode, boolean available, String summary) {
        this.condition = condition;
        this.score = score;
        this.temperatureCelsius = temperatureCelsius;
        this.precipitationMm = precipitationMm;
        this.rainMm = rainMm;
        this.windSpeedKmh = windSpeedKmh;
        this.weatherCode = weatherCode;
        this.available = available;
        this.summary = summary;
        this.coveragePercent = available ? 100.0 : 0.0;
    }

    public static WeatherInfo unavailable(String reason) {
        return new WeatherInfo("Weather data unavailable", 0.0, null, null, null, null, null, false, reason != null ? reason : "Weather data unavailable");
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

    public boolean isAvailable() {
        return available;
    }

    public void setAvailable(boolean available) {
        this.available = available;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public int getRainAffectedSegmentCount() {
        return rainAffectedSegmentCount;
    }

    public void setRainAffectedSegmentCount(int rainAffectedSegmentCount) {
        this.rainAffectedSegmentCount = rainAffectedSegmentCount;
    }

    public int getSevereWeatherSegmentCount() {
        return severeWeatherSegmentCount;
    }

    public void setSevereWeatherSegmentCount(int severeWeatherSegmentCount) {
        this.severeWeatherSegmentCount = severeWeatherSegmentCount;
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

    public Double getTemperatureMin() {
        return temperatureMin;
    }

    public void setTemperatureMin(Double temperatureMin) {
        this.temperatureMin = temperatureMin;
    }

    public Double getTemperatureMax() {
        return temperatureMax;
    }

    public void setTemperatureMax(Double temperatureMax) {
        this.temperatureMax = temperatureMax;
    }

    public Integer getPrecipitationProbabilityMax() {
        return precipitationProbabilityMax;
    }

    public void setPrecipitationProbabilityMax(Integer precipitationProbabilityMax) {
        this.precipitationProbabilityMax = precipitationProbabilityMax;
    }
}
