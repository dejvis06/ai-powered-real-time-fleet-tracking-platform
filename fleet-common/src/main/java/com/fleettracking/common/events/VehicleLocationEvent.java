package com.fleettracking.common.events;

import java.time.Instant;
import java.util.UUID;

/**
 * Published to Kafka when the MQTT ingestion service receives a vehicle location update.
 */
public record VehicleLocationEvent(
        UUID eventId,
        UUID deliveryId,
        UUID vehicleId,
        double latitude,
        double longitude,
        double heading,
        double speedKph,
        Instant recordedAt,
        int schemaVersion
) {
    public VehicleLocationEvent(
            UUID eventId,
            UUID deliveryId,
            UUID vehicleId,
            double latitude,
            double longitude,
            double heading,
            double speedKph,
            Instant recordedAt
    ) {
        this(eventId, deliveryId, vehicleId, latitude, longitude, heading, speedKph, recordedAt, 1);
    }
}
