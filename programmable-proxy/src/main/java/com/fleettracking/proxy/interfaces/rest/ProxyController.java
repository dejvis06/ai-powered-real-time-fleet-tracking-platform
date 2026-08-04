package com.fleettracking.proxy.interfaces.rest;

import com.fleettracking.proxy.application.service.ProxyStreamService;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.http.MediaType;
import org.springframework.http.codec.ServerSentEvent;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Flux;

import java.util.UUID;

/**
 * Single SSE endpoint exposed to browser clients.
 * The client opens one EventSource connection here and receives all events
 * (ETA_UPDATED, DELIVERY_COMPLETED, DELIVERY_FAILED) for the given delivery.
 */
@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/proxy")
public class ProxyController {

    private final ProxyStreamService proxyStreamService;
    private final MeterRegistry meterRegistry;

    public ProxyController(ProxyStreamService proxyStreamService, MeterRegistry meterRegistry) {
        this.proxyStreamService = proxyStreamService;
        this.meterRegistry = meterRegistry;
    }

    @GetMapping(value = "/deliveries/{deliveryId}/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public Flux<ServerSentEvent<String>> stream(@PathVariable UUID deliveryId) {
        meterRegistry.counter("sse.connections.active").increment();
        return proxyStreamService.streamForDelivery(deliveryId)
                .doFinally(signal -> meterRegistry.counter("sse.connections.active").increment(-1));
    }
}
