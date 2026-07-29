# MQTT Ingestion Service

## Purpose

Bridges the MQTT broker and Apache Kafka. It listens for raw vehicle location messages published over MQTT and republishes them as structured `VehicleLocationEvent` records on Kafka, keyed by `deliveryId` so that Kafka partitions them correctly.

---

## Responsibilities

- Subscribe to the `fleet/vehicles/+/location` MQTT topic
- Deserialize incoming JSON payloads into `RawLocationMessage` objects
- Publish `VehicleLocationEvent` to the `vehicle-location` Kafka topic (key = `deliveryId`)
- Track `mqtt.messages.received` and `mqtt.messages.rejected` metrics

---

## Component Overview

```
MQTT Broker
      │
      ▼
MqttIngestionConfig          (infrastructure/mqtt)
(Spring Integration adapter)
      │
      ▼
LocationIngestionService     (application)
      │
      ▼
KafkaLocationEventPublisher  (infrastructure/messaging)
      │
      ▼
Kafka  →  topic: vehicle-location
           key:   deliveryId
```

---

## Startup Flow

```
Spring Boot starts (no web server)
      │
      ▼
MQTT client connects to broker with QoS 1 and clean-session: false
      │
      ▼
Subscribes to fleet/vehicles/+/location
      │
      ▼
Kafka producer initialised
      │
      ▼
Ready to ingest messages
```

---

## Processing Flow

```
Vehicle publishes location
      │
      ▼
MQTT Broker
      │
      ▼
MQTT Ingestion Service (MqttIngestionConfig)
      │
      ▼
Deserialize JSON → RawLocationMessage
      │
      ▼
LocationIngestionService.ingest()
      │
      ▼
KafkaLocationEventPublisher.publish()
      │
      ▼
Kafka  topic: vehicle-location
       key:   deliveryId   (ensures same partition per delivery)
```

---

## Interaction with Other Services

- **MQTT Broker (Mosquitto)**: subscribes to vehicle location topics.
- **Kafka**: produces to `vehicle-location`. The ETA Service consumes from this topic.

---

## Configuration

| Property                         | Default             | Description                    |
|----------------------------------|---------------------|--------------------------------|
| `mqtt.broker-url`                | tcp://localhost:1883| Mosquitto connection URL       |
| `mqtt.client-id`                 | mqtt-ingestion-service | MQTT client identifier      |
| `spring.kafka.bootstrap-servers` | localhost:9092      | Kafka brokers                  |

---

## Running the Service

```bash
mvn spring-boot:run -pl mqtt-ingestion-service
```
