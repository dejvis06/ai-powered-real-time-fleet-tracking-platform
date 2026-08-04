package com.fleettracking.ingestion.infrastructure.mqtt;

import tools.jackson.databind.ObjectMapper;
import com.fleettracking.ingestion.application.service.LocationIngestionService;
import com.fleettracking.ingestion.domain.model.RawLocationMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;

@Component
public class MqttMessageHandler {

    private static final Logger log = LoggerFactory.getLogger(MqttMessageHandler.class);

    private final LocationIngestionService ingestionService;
    private final ObjectMapper objectMapper;

    public MqttMessageHandler(LocationIngestionService ingestionService, ObjectMapper objectMapper) {
        this.ingestionService = ingestionService;
        this.objectMapper = objectMapper;
    }

    @ServiceActivator(inputChannel = "mqttInputChannel")
    public void handle(Message<?> message) {
        try {
            byte[] raw = (byte[]) message.getPayload();
            String payload = new String(raw, StandardCharsets.UTF_8);
            RawLocationMessage locationMessage = objectMapper.readValue(payload, RawLocationMessage.class);
            ingestionService.ingest(locationMessage);
        } catch (Exception e) {
            log.error("Failed to process MQTT message: {}", message.getPayload(), e);
        }
    }
}
