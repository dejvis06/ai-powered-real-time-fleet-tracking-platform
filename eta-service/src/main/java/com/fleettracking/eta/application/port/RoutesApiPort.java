package com.fleettracking.eta.application.port;

import reactor.core.publisher.Mono;

/**
 * Port for the Google Routes API (reactive).
 */
public interface RoutesApiPort {

    Mono<RouteResult> computeRoute(
            double originLat, double originLon,
            double destLat, double destLon
    );

    record RouteResult(
            long distanceMeters,
            long durationSeconds
    ) {}
}
