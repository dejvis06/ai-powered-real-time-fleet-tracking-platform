package com.fleettracking.delivery.domain.model;

/**
 * Value object representing a geographical coordinate.
 */
public record GeoPoint(double latitude, double longitude) {

    public com.fleettracking.common.model.GeoPoint toCommon() {
        return new com.fleettracking.common.model.GeoPoint(latitude, longitude);
    }

    public static GeoPoint from(com.fleettracking.common.model.GeoPoint common) {
        return new GeoPoint(common.latitude(), common.longitude());
    }
}
