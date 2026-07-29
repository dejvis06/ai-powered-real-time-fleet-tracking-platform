package com.fleettracking.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published when a delivery transitions to ACTIVE state.
 */
public record DeliveryStartedEvent(
        UUID eventId,
        UUID deliveryId,
        UUID vehicleId,
        double originLatitude,
        double originLongitude,
        double destinationLatitude,
        double destinationLongitude,
        Instant startedAt,
        int schemaVersion
) {
    public DeliveryStartedEvent(
            UUID eventId,
            UUID deliveryId,
            UUID vehicleId,
            double originLatitude,
            double originLongitude,
            double destinationLatitude,
            double destinationLongitude,
            Instant startedAt
    ) {
        this(eventId, deliveryId, vehicleId, originLatitude, originLongitude,
                destinationLatitude, destinationLongitude, startedAt, 1);
    }
}
