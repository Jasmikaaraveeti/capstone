package com.intelliroute.model;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class RouteRequest {
    @NotBlank(message = "Source location is required")
    private String source;

    @NotBlank(message = "Destination location is required")
    private String destination;

    private Double sourceLat;
    private Double sourceLon;
    private Double destLat;
    private Double destLon;

    @NotNull(message = "Vehicle type is required")
    private VehicleType vehicle = VehicleType.CAR;

    @NotNull(message = "Route preference is required")
    private PreferenceType preference = PreferenceType.RECOMMENDED;

    public RouteRequest() {
    }

    public RouteRequest(String source, String destination, VehicleType vehicle, PreferenceType preference) {
        this.source = source;
        this.destination = destination;
        this.vehicle = vehicle;
        this.preference = preference;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public Double getSourceLat() {
        return sourceLat;
    }

    public void setSourceLat(Double sourceLat) {
        this.sourceLat = sourceLat;
    }

    public Double getSourceLon() {
        return sourceLon;
    }

    public void setSourceLon(Double sourceLon) {
        this.sourceLon = sourceLon;
    }

    public Double getDestLat() {
        return destLat;
    }

    public void setDestLat(Double destLat) {
        this.destLat = destLat;
    }

    public Double getDestLon() {
        return destLon;
    }

    public void setDestLon(Double destLon) {
        this.destLon = destLon;
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
}
