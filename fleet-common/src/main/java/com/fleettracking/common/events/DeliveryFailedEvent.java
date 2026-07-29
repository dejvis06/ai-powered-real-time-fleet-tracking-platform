package com.fleettracking.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a delivery transitions to FAILED state.
 */
public record DeliveryFailedEvent(
        UUID eventId,
        UUID deliveryId,
        UUID vehicleId,
        String reason,
        Instant failedAt,
        int schemaVersion
) {
    public DeliveryFailedEvent(
            UUID eventId,
            UUID deliveryId,
            UUID vehicleId,
            String reason,
            Instant failedAt
    ) {
        this(eventId, deliveryId, vehicleId, reason, failedAt, 1);
    }
}
