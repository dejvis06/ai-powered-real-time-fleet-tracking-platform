package com.fleettracking.delivery.application.port;

import com.fleettracking.delivery.domain.model.Delivery;

/**
 * Port for publishing delivery lifecycle events to Kafka and updating Redis.
 */
public interface DeliveryStatePublisher {

    void publishStarted(Delivery delivery);

    void publishCompleted(Delivery delivery);

    void publishFailed(Delivery delivery);
}
