package com.fleettracking.eta.application.port;

import com.fleettracking.common.model.DeliveryStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port for reading delivery state from the distributed Redis cache.
 */
public interface DeliveryStateCache {

    Mono<DeliveryStatus> getStatus(UUID deliveryId);

    Mono<DeliveryDestination> getDestination(UUID deliveryId);

    record DeliveryDestination(double latitude, double longitude) {}
}
