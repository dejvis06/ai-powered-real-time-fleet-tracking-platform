package com.fleettracking.delivery.domain.service;

import com.fleettracking.delivery.domain.model.Delivery;
import com.fleettracking.delivery.domain.model.DeliveryStatus;

/**
 * Default domain service implementation — pure business logic only.
 */
public class DefaultDeliveryDomainService implements DeliveryDomainService {

    @Override
    public void validateStart(Delivery delivery) {
        if (delivery.getStatus() != DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Only PENDING deliveries can be started. Current status: " + delivery.getStatus());
        }
    }
}
