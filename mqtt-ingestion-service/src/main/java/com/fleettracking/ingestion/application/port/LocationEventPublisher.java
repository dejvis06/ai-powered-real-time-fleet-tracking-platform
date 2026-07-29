package com.fleettracking.ingestion.application.port;

import com.fleettracking.ingestion.domain.model.RawLocationMessage;

/**
 * Port for publishing vehicle location events to Kafka.
 */
public interface LocationEventPublisher {

    void publish(RawLocationMessage message);
}
