package com.fleettracking.delivery.domain.model;

import java.util.UUID;

/**
 * Value object representing a vehicle's unique identity.
 */
public record VehicleId(UUID value) {

    public static VehicleId of(UUID value) {
        return new VehicleId(value);
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
