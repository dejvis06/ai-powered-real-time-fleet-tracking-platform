package com.fleettracking.proxy.application.service;

import com.fleettracking.common.model.DeliveryStatus;
import com.fleettracking.common.topics.KafkaTopics;
import com.fleettracking.proxy.application.port.DeliveryStateReader;
import com.fleettracking.proxy.application.port.UpstreamSseClient;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Core proxy logic:
 * 1. Subscribe to eta-service SSE and delivery-service SSE for the given delivery.
 * 2. Merge both streams into a single outbound stream for the browser client.
 * 3. Filter: drop ETA_UPDATED events when delivery state is COMPLETED or FAILED (check Redis).
 * 4. Terminate the stream when DELIVERY_COMPLETED or DELIVERY_FAILED is received.
 */
@Service
public class ProxyStreamService {

    private static final Logger log = LoggerFactory.getLogger(ProxyStreamService.class);

    private final UpstreamSseClient upstreamSseClient;
    private final DeliveryStateReader deliveryStateReader;
    private final MeterRegistry meterRegistry;

    public ProxyStreamService(
            UpstreamSseClient upstreamSseClient,
            DeliveryStateReader deliveryStateReader,
            MeterRegistry meterRegistry
    ) {
        this.upstreamSseClient = upstreamSseClient;
        this.deliveryStateReader = deliveryStateReader;
        this.meterRegistry = meterRegistry;
    }

    /**
     * Returns the merged, filtered SSE stream for the browser client.
     */
    public Flux<ServerSentEvent<String>> streamForDelivery(UUID deliveryId) {
        Flux<ServerSentEvent<String>> etaStream = upstreamSseClient.subscribeEtaStream(deliveryId)
                .filterWhen(event -> shouldRelayEtaEvent(deliveryId, event))
                .doOnNext(e -> meterRegistry.counter("sse.events.forwarded").increment());

        Flux<ServerSentEvent<String>> deliveryStream = upstreamSseClient.subscribeDeliveryStream(deliveryId)
                .doOnNext(e -> meterRegistry.counter("sse.events.forwarded").increment());

        return Flux.merge(etaStream, deliveryStream)
                .takeUntil(this::isTerminalEvent)
                .doOnCancel(() -> log.debug("Client disconnected from delivery={}", deliveryId))
                .doOnComplete(() -> log.info("Stream completed for delivery={}", deliveryId));
    }

    private reactor.core.publisher.Mono<Boolean> shouldRelayEtaEvent(
            UUID deliveryId, ServerSentEvent<String> event
    ) {
        if (!KafkaTopics.ETA_UPDATED.equals(event.event())) {
            return reactor.core.publisher.Mono.just(true);
        }

        return deliveryStateReader.getStatus(deliveryId)
                .map(status -> {
                    boolean active = status == DeliveryStatus.ACTIVE;
                    if (!active) {
                        log.debug("Dropping ETA_UPDATED for delivery={} status={}", deliveryId, status);
                        meterRegistry.counter("sse.events.dropped").increment();
                    }
                    return active;
                })
                .defaultIfEmpty(true); // if unknown, relay (delivery-service will handle)
    }

    private boolean isTerminalEvent(ServerSentEvent<String> event) {
        return KafkaTopics.DELIVERY_COMPLETED.equals(event.event()) || KafkaTopics.DELIVERY_FAILED.equals(event.event());
    }
}
