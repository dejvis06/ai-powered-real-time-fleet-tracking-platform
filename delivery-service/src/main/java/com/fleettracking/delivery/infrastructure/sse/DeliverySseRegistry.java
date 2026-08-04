package com.fleettracking.delivery.infrastructure.sse;

import com.fleettracking.delivery.application.port.DeliveryEventEmitter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;

/**
 * Registry of SSE emitters keyed by deliveryId.
 * Implements DeliveryEventEmitter so the application layer can notify subscribers
 * without depending on the infrastructure layer.
 */
@Component
public class DeliverySseRegistry implements DeliveryEventEmitter {

    private static final Logger log = LoggerFactory.getLogger(DeliverySseRegistry.class);

    /** deliveryId → list of active emitters (multiple browser tabs / proxy instances) */
    private final ConcurrentHashMap<UUID, List<SseEmitter>> emitters = new ConcurrentHashMap<>();

    /**
     * Create and register a new SSE emitter for a delivery.
     * Timeout of 0 = never expire; proxy / browser handles reconnect.
     */
    public SseEmitter subscribe(UUID deliveryId) {
        SseEmitter emitter = new SseEmitter(0L);
        emitters.computeIfAbsent(deliveryId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        Runnable cleanup = () -> {
            List<SseEmitter> list = emitters.get(deliveryId);
            if (list != null) {
                list.remove(emitter);
                if (list.isEmpty()) emitters.remove(deliveryId);
            }
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    @Override
    public void emitStarted(UUID deliveryId) {
        emit(deliveryId, "DELIVERY_STARTED", "{\"deliveryId\":\"" + deliveryId + "\"}");
    }

    @Override
    public void emitCompleted(UUID deliveryId) {
        emit(deliveryId, "DELIVERY_COMPLETED", "{\"deliveryId\":\"" + deliveryId + "\"}");
    }

    @Override
    public void emitFailed(UUID deliveryId) {
        emit(deliveryId, "DELIVERY_FAILED", "{\"deliveryId\":\"" + deliveryId + "\",\"reason\":\"Delivery failed\"}");
    }

    /** Emit a named event to all subscribers of a delivery. */
    public void emit(UUID deliveryId, String eventName, String data) {
        List<SseEmitter> list = emitters.get(deliveryId);
        if (list == null || list.isEmpty()) return;

        SseEmitter.SseEventBuilder event = SseEmitter.event()
                .name(eventName)
                .data(data);

        List<SseEmitter> dead = new CopyOnWriteArrayList<>();
        for (SseEmitter emitter : list) {
            try {
                emitter.send(event);
                if ("DELIVERY_COMPLETED".equals(eventName) || "DELIVERY_FAILED".equals(eventName)) {
                    emitter.complete();
                    dead.add(emitter);
                }
            } catch (IOException e) {
                log.debug("SSE emitter disconnected for delivery={}", deliveryId);
                dead.add(emitter);
            }
        }
        list.removeAll(dead);
        if (list.isEmpty()) emitters.remove(deliveryId);
    }
}
