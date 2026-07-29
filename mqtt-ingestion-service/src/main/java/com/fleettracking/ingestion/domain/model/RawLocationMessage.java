package com.fleettracking.ingestion.domain.model;

import java.time.Instant;
import java.util.UUID;

/**
 * Domain model representing a raw vehicle location received over MQTT.
 */
public record RawLocationMessage(
        UUID vehicleId,
        UUID deliveryId,
        double latitude,
        double longitude,
        double heading,
        double speedKph,
        Instant recordedAt
) {
}
