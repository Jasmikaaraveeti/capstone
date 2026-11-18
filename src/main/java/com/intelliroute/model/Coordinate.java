package com.intelliroute.model;

public class Coordinate {
    private String name;
    private String district;
    private double latitude;
    private double longitude;

    private String state;
    private String displayName;

    public Coordinate() {
    }

    public Coordinate(String name, String district, double latitude, double longitude) {
        this.name = name;
        this.district = district;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public Coordinate(String name, String district, String state, double latitude, double longitude) {
        this.name = name;
        this.district = district;
        this.state = state;
        this.latitude = latitude;
        this.longitude = longitude;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDistrict() {
        return district;
    }

    public void setDistrict(String district) {
        this.district = district;
    }

    public String getState() {
        return state;
    }

    public void setState(String state) {
        this.state = state;
    }

    public double getLatitude() {
        return latitude;
    }

    public void setLatitude(double latitude) {
        this.latitude = latitude;
    }

    public double getLongitude() {
        return longitude;
    }

    public void setLongitude(double longitude) {
        this.longitude = longitude;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        if (displayName != null && !displayName.isBlank()) {
            return displayName;
        }
        StringBuilder sb = new StringBuilder(name != null ? name : "");
        if (district != null && !district.isBlank()) {
            sb.append(", ").append(district);
        }
        if (state != null && !state.isBlank()) {
            sb.append(", ").append(state);
        }
        sb.append(", India");
        return sb.toString();
    }
}
