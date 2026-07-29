package com.fleettracking.proxy.application.port;

import com.fleettracking.common.model.DeliveryStatus;
import reactor.core.publisher.Mono;

import java.util.UUID;

/**
 * Port for reading delivery state from the distributed Redis cache.
 */
public interface DeliveryStateReader {

    Mono<DeliveryStatus> getStatus(UUID deliveryId);
}
