package com.fleettracking.ingestion.infrastructure.messaging;

import com.fleettracking.common.events.VehicleLocationEvent;
import com.fleettracking.common.topics.KafkaTopics;
import com.fleettracking.ingestion.application.port.LocationEventPublisher;
import com.fleettracking.ingestion.domain.model.RawLocationMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;

@Component
public class KafkaLocationEventPublisher implements LocationEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(KafkaLocationEventPublisher.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final MeterRegistry meterRegistry;

    public KafkaLocationEventPublisher(
            KafkaTemplate<String, Object> kafkaTemplate,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publish(RawLocationMessage message) {
        VehicleLocationEvent event = new VehicleLocationEvent(
                UUID.randomUUID(),
                message.deliveryId(),
                message.vehicleId(),
                message.latitude(),
                message.longitude(),
                message.heading(),
                message.speedKph(),
                message.recordedAt()
        );

        String key = message.deliveryId().toString(); // partition by deliveryId

        kafkaTemplate.send(KafkaTopics.VEHICLE_LOCATION, key, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish VehicleLocationEvent delivery={} vehicle={}",
                                message.deliveryId(), message.vehicleId(), ex);
                        meterRegistry.counter("kafka.events.failed",
                                "topic", KafkaTopics.VEHICLE_LOCATION).increment();
                    } else {
                        log.debug("Published VehicleLocationEvent delivery={} partition={}",
                                message.deliveryId(),
                                result.getRecordMetadata().partition());
                        meterRegistry.counter("kafka.events.published",
                                "topic", KafkaTopics.VEHICLE_LOCATION).increment();
                    }
                });
    }
}
