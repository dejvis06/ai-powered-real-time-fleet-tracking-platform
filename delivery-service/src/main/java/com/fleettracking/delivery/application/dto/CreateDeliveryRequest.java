package com.fleettracking.delivery.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.List;
import java.util.UUID;

public record CreateDeliveryRequest(
        @NotNull UUID vehicleId,
        @NotNull Double originLatitude,
        @NotNull Double originLongitude,
        @NotNull Double destinationLatitude,
        @NotNull Double destinationLongitude,
        List<WaypointDto> waypoints
) {
    public record WaypointDto(double latitude, double longitude) {}
}
