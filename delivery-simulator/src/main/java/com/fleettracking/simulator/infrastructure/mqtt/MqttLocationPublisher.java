package com.fleettracking.simulator.infrastructure.mqtt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.fleettracking.common.topics.MqttTopics;
import com.fleettracking.simulator.application.port.LocationPublisher;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import org.eclipse.paho.client.mqttv3.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
public class MqttLocationPublisher implements LocationPublisher {

    private static final Logger log = LoggerFactory.getLogger(MqttLocationPublisher.class);

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule());

    @Value("${mqtt.broker-url:tcp://localhost:1883}")
    private String brokerUrl;

    @Value("${mqtt.client-id:delivery-simulator}")
    private String clientId;

    private MqttClient mqttClient;

    @PostConstruct
    public void connect() throws MqttException {
        mqttClient = new MqttClient(brokerUrl, clientId);
        MqttConnectOptions options = new MqttConnectOptions();
        options.setCleanSession(true);
        options.setAutomaticReconnect(true);
        mqttClient.connect(options);
        log.info("Connected to MQTT broker: {}", brokerUrl);
    }

    @PreDestroy
    public void disconnect() throws MqttException {
        if (mqttClient != null && mqttClient.isConnected()) {
            mqttClient.disconnect();
        }
    }

    @Override
    public void publishLocation(
            UUID vehicleId,
            UUID deliveryId,
            double latitude,
            double longitude,
            double heading,
            double speedKph
    ) {
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("vehicleId", vehicleId.toString());
            payload.put("deliveryId", deliveryId.toString());
            payload.put("latitude", latitude);
            payload.put("longitude", longitude);
            payload.put("heading", heading);
            payload.put("speedKph", speedKph);
            payload.put("recordedAt", Instant.now().toString());

            byte[] bytes = objectMapper.writeValueAsBytes(payload);
            String topic = MqttTopics.vehicleLocation(vehicleId.toString());

            MqttMessage message = new MqttMessage(bytes);
            message.setQos(1);
            mqttClient.publish(topic, message);

            log.debug("Published location for vehicle={} delivery={} lat={} lon={}",
                    vehicleId, deliveryId, latitude, longitude);

        } catch (Exception e) {
            log.error("Failed to publish MQTT location for vehicle={}", vehicleId, e);
        }
    }
}
