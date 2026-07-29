package com.fleettracking.delivery.application.dto;

import com.fleettracking.delivery.domain.model.DeliveryStatus;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record DeliveryResponse(
        UUID deliveryId,
        UUID vehicleId,
        DeliveryStatus status,
        Double originLatitude,
        Double originLongitude,
        Double destinationLatitude,
        Double destinationLongitude,
        List<WaypointDto> waypoints,
        String encodedPolyline,
        Instant startedAt,
        Instant completedAt,
        Instant failedAt,
        String failureReason,
        Instant createdAt
) {
    public record WaypointDto(double latitude, double longitude) {}
}
