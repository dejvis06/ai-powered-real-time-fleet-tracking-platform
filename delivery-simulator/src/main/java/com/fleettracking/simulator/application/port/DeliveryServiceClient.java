package com.fleettracking.simulator.application.port;

import java.util.List;
import java.util.UUID;

/**
 * Port for interacting with the delivery-service REST API.
 */
public interface DeliveryServiceClient {

    CreateDeliveryResult createDelivery(
            UUID vehicleId,
            double originLat, double originLon,
            double destLat, double destLon
    );

    void startDelivery(UUID deliveryId);

    void completeDelivery(UUID deliveryId);

    void failDelivery(UUID deliveryId, String reason);

    record CreateDeliveryResult(UUID deliveryId, List<Waypoint> waypoints) {}
    record Waypoint(double latitude, double longitude) {}
}
