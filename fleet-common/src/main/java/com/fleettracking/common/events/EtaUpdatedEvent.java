package com.fleettracking.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published by the ETA service when a new ETA has been calculated.
 */
public record EtaUpdatedEvent(
        UUID eventId,
        UUID deliveryId,
        UUID vehicleId,
        double currentLatitude,
        double currentLongitude,
        Instant estimatedArrival,
        long remainingDistanceMeters,
        long remainingDurationSeconds,
        Instant calculatedAt,
        int schemaVersion
) {
    public EtaUpdatedEvent(
            UUID eventId,
            UUID deliveryId,
            UUID vehicleId,
            double currentLatitude,
            double currentLongitude,
            Instant estimatedArrival,
            long remainingDistanceMeters,
            long remainingDurationSeconds,
            Instant calculatedAt
    ) {
        this(eventId, deliveryId, vehicleId, currentLatitude, currentLongitude,
                estimatedArrival, remainingDistanceMeters, remainingDurationSeconds, calculatedAt, 1);
    }
}
