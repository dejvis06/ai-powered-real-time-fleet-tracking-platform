package com.fleettracking.delivery.infrastructure.messaging;

import com.fleettracking.common.events.DeliveryCompletedEvent;
import com.fleettracking.common.events.DeliveryFailedEvent;
import com.fleettracking.common.events.DeliveryStartedEvent;
import com.fleettracking.common.model.DeliveryStatus;
import com.fleettracking.common.topics.KafkaTopics;
import com.fleettracking.delivery.application.port.DeliveryStatePublisher;
import com.fleettracking.delivery.domain.model.Delivery;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
public class DeliveryStatePublisherImpl implements DeliveryStatePublisher {

    private static final Logger log = LoggerFactory.getLogger(DeliveryStatePublisherImpl.class);

    private final KafkaTemplate<String, Object> kafkaTemplate;
    private final StringRedisTemplate redisTemplate;
    private final MeterRegistry meterRegistry;

    public DeliveryStatePublisherImpl(
            KafkaTemplate<String, Object> kafkaTemplate,
            StringRedisTemplate redisTemplate,
            MeterRegistry meterRegistry
    ) {
        this.kafkaTemplate = kafkaTemplate;
        this.redisTemplate = redisTemplate;
        this.meterRegistry = meterRegistry;
    }

    @Override
    public void publishStarted(Delivery delivery) {
        String deliveryId = delivery.getId().toString();
        String vehicleId = delivery.getVehicleId().toString();

        // Update Redis: deliveryId -> ACTIVE
        redisTemplate.opsForValue().set("delivery:status:" + deliveryId, DeliveryStatus.ACTIVE.name());

        DeliveryStartedEvent event = new DeliveryStartedEvent(
                UUID.randomUUID(),
                delivery.getId().value(),
                delivery.getVehicleId().value(),
                delivery.getOrigin().latitude(),
                delivery.getOrigin().longitude(),
                delivery.getDestination().latitude(),
                delivery.getDestination().longitude(),
                delivery.getStartedAt()
        );

        kafkaTemplate.send(KafkaTopics.DELIVERY_STARTED, deliveryId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DELIVERY_STARTED for delivery={}", deliveryId, ex);
                        meterRegistry.counter("kafka.events.failed",
                                "topic", KafkaTopics.DELIVERY_STARTED,
                                "deliveryId", deliveryId).increment();
                    } else {
                        log.info("Published DELIVERY_STARTED delivery={} vehicle={}", deliveryId, vehicleId);
                        meterRegistry.counter("kafka.events.published",
                                "topic", KafkaTopics.DELIVERY_STARTED).increment();
                    }
                });
    }

    @Override
    public void publishCompleted(Delivery delivery) {
        String deliveryId = delivery.getId().toString();

        // Update Redis: deliveryId -> COMPLETED
        redisTemplate.opsForValue().set("delivery:status:" + deliveryId, DeliveryStatus.COMPLETED.name());

        DeliveryCompletedEvent event = new DeliveryCompletedEvent(
                UUID.randomUUID(),
                delivery.getId().value(),
                delivery.getVehicleId().value(),
                delivery.getCompletedAt()
        );

        kafkaTemplate.send(KafkaTopics.DELIVERY_COMPLETED, deliveryId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DELIVERY_COMPLETED for delivery={}", deliveryId, ex);
                        meterRegistry.counter("kafka.events.failed",
                                "topic", KafkaTopics.DELIVERY_COMPLETED).increment();
                    } else {
                        log.info("Published DELIVERY_COMPLETED delivery={}", deliveryId);
                        meterRegistry.counter("kafka.events.published",
                                "topic", KafkaTopics.DELIVERY_COMPLETED).increment();
                    }
                });
    }

    @Override
    public void publishFailed(Delivery delivery) {
        String deliveryId = delivery.getId().toString();

        // Update Redis: deliveryId -> FAILED
        redisTemplate.opsForValue().set("delivery:status:" + deliveryId, DeliveryStatus.FAILED.name());

        DeliveryFailedEvent event = new DeliveryFailedEvent(
                UUID.randomUUID(),
                delivery.getId().value(),
                delivery.getVehicleId().value(),
                delivery.getFailureReason(),
                delivery.getFailedAt()
        );

        kafkaTemplate.send(KafkaTopics.DELIVERY_FAILED, deliveryId, event)
                .whenComplete((result, ex) -> {
                    if (ex != null) {
                        log.error("Failed to publish DELIVERY_FAILED for delivery={}", deliveryId, ex);
                        meterRegistry.counter("kafka.events.failed",
                                "topic", KafkaTopics.DELIVERY_FAILED).increment();
                    } else {
                        log.info("Published DELIVERY_FAILED delivery={}", deliveryId);
                        meterRegistry.counter("kafka.events.published",
                                "topic", KafkaTopics.DELIVERY_FAILED).increment();
                    }
                });
    }
}
