package com.fleettracking.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a delivery transitions to COMPLETED state.
 */
public record DeliveryCompletedEvent(
        UUID eventId,
        UUID deliveryId,
        UUID vehicleId,
        Instant completedAt,
        int schemaVersion
) {
    public DeliveryCompletedEvent(
            UUID eventId,
            UUID deliveryId,
            UUID vehicleId,
            Instant completedAt
    ) {
        this(eventId, deliveryId, vehicleId, completedAt, 1);
    }
}
