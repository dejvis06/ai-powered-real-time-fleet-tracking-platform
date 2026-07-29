package com.fleettracking.delivery.domain.model;

/**
 * Lifecycle states for a Delivery aggregate.
 */
public enum DeliveryStatus {
    PENDING,
    ACTIVE,
    COMPLETED,
    FAILED
}
