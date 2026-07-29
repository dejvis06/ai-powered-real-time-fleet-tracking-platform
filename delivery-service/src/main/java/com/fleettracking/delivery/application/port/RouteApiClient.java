package com.fleettracking.delivery.application.port;

import com.fleettracking.delivery.domain.model.GeoPoint;

import java.util.List;

/**
 * Port for the Google Routes API.
 * The infrastructure layer provides the actual HTTP implementation.
 */
public interface RouteApiClient {

    /**
     * Fetches a route from origin to destination, optionally via waypoints.
     *
     * @return encoded polyline of the computed route
     */
    RouteResult computeRoute(GeoPoint origin, GeoPoint destination, List<GeoPoint> waypoints);

    record RouteResult(
            String encodedPolyline,
            List<GeoPoint> decodedWaypoints,
            long distanceMeters,
            long durationSeconds
    ) {}
}
