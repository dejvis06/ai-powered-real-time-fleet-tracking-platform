package com.fleettracking.common.model;

import java.util.List;

/**
 * A named route consisting of an ordered list of waypoints.
 */
public record RouteDefinition(
        String name,
        GeoPoint origin,
        GeoPoint destination,
        List<GeoPoint> waypoints,
        String encodedPolyline
) {
}
