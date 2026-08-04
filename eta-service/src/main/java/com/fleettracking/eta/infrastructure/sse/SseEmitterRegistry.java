package com.fleettracking.eta.infrastructure.sse;

import com.fleettracking.common.topics.KafkaTopics;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Manages per-delivery SSE sinks.
 * Each delivery has a multicast sink; connected clients subscribe to receive ETA_UPDATED events.
 */
@Component
public class SseEmitterRegistry {

    private static final Logger log = LoggerFactory.getLogger(SseEmitterRegistry.class);

    private final Map<UUID, Sinks.Many<ServerSentEvent<Object>>> sinks = new ConcurrentHashMap<>();
    private final MeterRegistry meterRegistry;

    public SseEmitterRegistry(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    /**
     * Returns a Flux of SSE events for the given delivery.
     * Creates a new sink if one does not yet exist.
     */
    public Flux<ServerSentEvent<Object>> subscribe(UUID deliveryId) {
        Sinks.Many<ServerSentEvent<Object>> sink = sinks.computeIfAbsent(deliveryId,
                id -> Sinks.many().multicast().onBackpressureBuffer());

        meterRegistry.gauge("sse.connections.active", sinks, Map::size);

        return sink.asFlux()
                .doOnCancel(() -> {
                    log.debug("SSE client disconnected from delivery={}", deliveryId);
                    meterRegistry.counter("sse.events.dropped").increment();
                });
    }

    /**
     * Emits an event to all subscribers for the given delivery.
     */
    public void emit(UUID deliveryId, Object payload) {
        Sinks.Many<ServerSentEvent<Object>> sink = sinks.get(deliveryId);
        if (sink == null) {
            log.debug("No SSE subscribers for delivery={}", deliveryId);
            return;
        }

        ServerSentEvent<Object> event = ServerSentEvent.builder()
                .id(UUID.randomUUID().toString())
                .event(KafkaTopics.ETA_UPDATED)
                .data(payload)
                .build();

        Sinks.EmitResult result = sink.tryEmitNext(event);
        if (result.isSuccess()) {
            meterRegistry.counter("sse.events.forwarded").increment();
        } else {
            log.warn("Failed to emit SSE event for delivery={} result={}", deliveryId, result);
            meterRegistry.counter("sse.events.dropped").increment();
        }
    }

    /**
     * Terminates the sink for a delivery (called on COMPLETED or FAILED).
     */
    public void complete(UUID deliveryId) {
        Sinks.Many<ServerSentEvent<Object>> sink = sinks.remove(deliveryId);
        if (sink != null) {
            sink.tryEmitComplete();
            log.info("Closed SSE sink for delivery={}", deliveryId);
        }
    }
}
