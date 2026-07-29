package com.fleettracking.delivery.application.service;

import java.util.UUID;

public class DeliveryNotFoundException extends RuntimeException {

    public DeliveryNotFoundException(UUID deliveryId) {
        super("Delivery not found: " + deliveryId);
    }
}
