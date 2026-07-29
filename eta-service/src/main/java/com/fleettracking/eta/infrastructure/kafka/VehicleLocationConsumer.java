package com.fleettracking.eta.infrastructure.kafka;

import com.fleettracking.common.events.VehicleLocationEvent;
import com.fleettracking.common.topics.KafkaTopics;
import com.fleettracking.eta.application.service.EtaApplicationService;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Component;

/**
 * Kafka consumer for VEHICLE_LOCATION events.
 * Each instance owns a subset of partitions (Kafka consumer group + partitioned by deliveryId).
 */
@Component
public class VehicleLocationConsumer {

    private static final Logger log = LoggerFactory.getLogger(VehicleLocationConsumer.class);

    private final EtaApplicationService etaApplicationService;
    private final MeterRegistry meterRegistry;

    public VehicleLocationConsumer(EtaApplicationService etaApplicationService, MeterRegistry meterRegistry) {
        this.etaApplicationService = etaApplicationService;
        this.meterRegistry = meterRegistry;
    }

    @KafkaListener(
            topics = KafkaTopics.VEHICLE_LOCATION,
            groupId = "${spring.kafka.consumer.group-id:eta-service}",
            containerFactory = "kafkaListenerContainerFactory"
    )
    public void consume(
            @Payload VehicleLocationEvent event,
            @Header(KafkaHeaders.RECEIVED_PARTITION) int partition,
            @Header(KafkaHeaders.OFFSET) long offset
    ) {
        log.debug("Received VehicleLocationEvent delivery={} vehicle={} partition={} offset={}",
                event.deliveryId(), event.vehicleId(), partition, offset);

        etaApplicationService.processLocationEvent(event)
                .subscribe(
                        null,
                        e -> {
                            log.error("Error processing VehicleLocationEvent delivery={}",
                                    event.deliveryId(), e);
                            meterRegistry.counter("kafka.events.failed",
                                    "topic", KafkaTopics.VEHICLE_LOCATION).increment();
                        },
                        () -> meterRegistry.counter("kafka.events.published",
                                "topic", KafkaTopics.VEHICLE_LOCATION).increment()
                );
    }
}
