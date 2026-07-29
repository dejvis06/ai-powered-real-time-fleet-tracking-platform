package com.fleettracking.delivery.domain.model;

import java.util.UUID;

/**
 * Value object representing the unique identity of a Delivery.
 */
public record DeliveryId(UUID value) {

    public static DeliveryId of(UUID value) {
        return new DeliveryId(value);
    }

    public static DeliveryId generate() {
        return new DeliveryId(UUID.randomUUID());
    }

    @Override
    public String toString() {
        return value.toString();
    }
}
