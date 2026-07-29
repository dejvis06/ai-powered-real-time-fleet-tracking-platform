package com.fleettracking.simulator.application.port;

import java.util.UUID;

/**
 * Port for publishing vehicle location updates over MQTT.
 */
public interface LocationPublisher {

    void publishLocation(
            UUID vehicleId,
            UUID deliveryId,
            double latitude,
            double longitude,
            double heading,
            double speedKph
    );
}
