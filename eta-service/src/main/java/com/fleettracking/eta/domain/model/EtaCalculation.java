package com.fleettracking.eta.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing the result of an ETA calculation.
 */
public record EtaCalculation(
        UUID deliveryId,
        UUID vehicleId,
        double currentLatitude,
        double currentLongitude,
        Instant estimatedArrival,
        long remainingDistanceMeters,
        long remainingDurationSeconds,
        Instant calculatedAt
) {
}
