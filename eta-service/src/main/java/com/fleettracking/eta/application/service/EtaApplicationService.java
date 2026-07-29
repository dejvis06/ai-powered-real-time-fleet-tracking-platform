package com.fleettracking.eta.application.service;

import com.fleettracking.common.events.EtaUpdatedEvent;
import com.fleettracking.common.events.VehicleLocationEvent;
import com.fleettracking.common.model.DeliveryStatus;
import com.fleettracking.eta.application.port.DeliveryStateCache;
import com.fleettracking.eta.application.port.RoutesApiPort;
import com.fleettracking.eta.infrastructure.sse.SseEmitterRegistry;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.UUID;

/**
 * Processes incoming vehicle location events:
 * 1. Checks Redis — if delivery is not ACTIVE, drop the event.
 * 2. Fetches destination from Redis.
 * 3. Calls Google Routes API to compute remaining ETA.
 * 4. Broadcasts ETA_UPDATED via SSE to connected clients.
 */
@Service
public class EtaApplicationService {

    private static final Logger log = LoggerFactory.getLogger(EtaApplicationService.class);

    private final DeliveryStateCache deliveryStateCache;
    private final RoutesApiPort routesApi;
    private final SseEmitterRegistry sseRegistry;
    private final MeterRegistry meterRegistry;

    public EtaApplicationService(
            DeliveryStateCache deliveryStateCache,
            RoutesApiPort routesApi,
            SseEmitterRegistry sseRegistry,
            MeterRegistry meterRegistry
    ) {
        this.deliveryStateCache = deliveryStateCache;
        this.routesApi = routesApi;
        this.sseRegistry = sseRegistry;
        this.meterRegistry = meterRegistry;
    }

    public Mono<Void> processLocationEvent(VehicleLocationEvent event) {
        Timer.Sample sample = Timer.start(meterRegistry);

        return deliveryStateCache.getStatus(event.deliveryId())
                .flatMap(status -> {
                    if (status != DeliveryStatus.ACTIVE) {
                        log.debug("Skipping ETA calculation for delivery={} status={}",
                                event.deliveryId(), status);
                        meterRegistry.counter("eta.calculations.skipped").increment();
                        return Mono.empty();
                    }
                    return calculateAndEmit(event, sample);
                })
                .onErrorResume(e -> {
                    log.error("Error processing location event for delivery={}", event.deliveryId(), e);
                    return Mono.empty();
                });
    }

    private Mono<Void> calculateAndEmit(VehicleLocationEvent event, Timer.Sample sample) {
        return deliveryStateCache.getDestination(event.deliveryId())
                .flatMap(destination -> routesApi.computeRoute(
                        event.latitude(), event.longitude(),
                        destination.latitude(), destination.longitude()
                ))
                .flatMap(route -> {
                    meterRegistry.counter("eta.calculations").increment();
                    sample.stop(meterRegistry.timer("eta.calculation.duration"));

                    Instant estimatedArrival = Instant.now()
                            .plusSeconds(route.durationSeconds());

                    EtaUpdatedEvent etaEvent = new EtaUpdatedEvent(
                            UUID.randomUUID(),
                            event.deliveryId(),
                            event.vehicleId(),
                            event.latitude(),
                            event.longitude(),
                            estimatedArrival,
                            route.distanceMeters(),
                            route.durationSeconds(),
                            Instant.now()
                    );

                    sseRegistry.emit(event.deliveryId(), etaEvent);

                    log.debug("ETA updated delivery={} eta={} distance={}m",
                            event.deliveryId(), estimatedArrival, route.distanceMeters());

                    return Mono.<Void>empty();
                });
    }
}
