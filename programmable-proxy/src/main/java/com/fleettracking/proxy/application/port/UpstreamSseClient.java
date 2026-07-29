package com.fleettracking.proxy.application.port;

import org.springframework.http.codec.ServerSentEvent;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Port for subscribing to SSE streams from upstream services.
 */
public interface UpstreamSseClient {

    /**
     * Subscribes to ETA_UPDATED events from the eta-service for the given delivery.
     */
    Flux<ServerSentEvent<String>> subscribeEtaStream(UUID deliveryId);

    /**
     * Subscribes to delivery lifecycle events (COMPLETED, FAILED) from the delivery-service.
     */
    Flux<ServerSentEvent<String>> subscribeDeliveryStream(UUID deliveryId);
}
