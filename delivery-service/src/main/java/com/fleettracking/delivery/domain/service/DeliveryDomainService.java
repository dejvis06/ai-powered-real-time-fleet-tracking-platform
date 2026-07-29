package com.fleettracking.delivery.domain.service;

import com.fleettracking.delivery.domain.model.Delivery;

/**
 * Domain service encapsulating business rules that span multiple aggregates
 * or require external information at the domain level.
 */
public interface DeliveryDomainService {

    /**
     * Validates that a delivery can be started (e.g., no other active delivery for the vehicle).
     */
    void validateStart(Delivery delivery);
}
