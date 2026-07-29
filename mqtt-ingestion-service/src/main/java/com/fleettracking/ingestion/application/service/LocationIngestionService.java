package com.fleettracking.ingestion.application.service;

import com.fleettracking.ingestion.application.port.LocationEventPublisher;
import com.fleettracking.ingestion.domain.model.RawLocationMessage;
import io.micrometer.core.instrument.MeterRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Application service that processes incoming MQTT messages and forwards them to Kafka.
 */
@Service
public class LocationIngestionService {

    private static final Logger log = LoggerFactory.getLogger(LocationIngestionService.class);

    private final LocationEventPublisher publisher;
    private final MeterRegistry meterRegistry;

    public LocationIngestionService(LocationEventPublisher publisher, MeterRegistry meterRegistry) {
        this.publisher = publisher;
        this.meterRegistry = meterRegistry;
    }

    public void ingest(RawLocationMessage message) {
        try {
            publisher.publish(message);
            meterRegistry.counter("mqtt.messages.received",
                    "vehicleId", message.vehicleId().toString()).increment();
        } catch (Exception e) {
            log.error("Failed to ingest location message for vehicle={} delivery={}",
                    message.vehicleId(), message.deliveryId(), e);
            meterRegistry.counter("mqtt.messages.rejected",
                    "vehicleId", message.vehicleId().toString()).increment();
            throw e;
        }
    }
}
