package com.fleettracking.eta.interfaces.rest;

import com.fleettracking.eta.infrastructure.sse.SseEmitterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * SSE endpoint — the programmable proxy subscribes here to get ETA_UPDATED events per delivery.
 */
@RestController
@RequestMapping("/deliveries")
public class EtaSseController {

    private final SseEmitterRegistry sseRegistry;

    public EtaSseController(SseEmitterRegistry sseRegistry) {
        this.sseRegistry = sseRegistry;
    }

    @GetMapping(value = "/{deliveryId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<Object>> streamEta(@PathVariable UUID deliveryId) {
        return sseRegistry.subscribe(deliveryId);
    }
}
