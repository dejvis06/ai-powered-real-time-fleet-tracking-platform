package com.fleettracking.delivery.application.port;

import java.util.UUID;

/**
 * Port: emit delivery lifecycle events to connected SSE subscribers.
 */
public interface DeliveryEventEmitter {
    void emitStarted(UUID deliveryId);
    void emitCompleted(UUID deliveryId);
    void emitFailed(UUID deliveryId);
}
