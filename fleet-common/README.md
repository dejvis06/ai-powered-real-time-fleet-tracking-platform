# Fleet Common

## Purpose

Shared library module. Contains all types shared across backend services: Kafka event records, domain model types, and topic/topic-pattern constants. No service should duplicate these definitions.

---

## Contents

### Events (`com.fleettracking.common.events`)

| Class                    | Published by           | Consumed by            |
|--------------------------|------------------------|------------------------|
| `VehicleLocationEvent`   | MQTT Ingestion Service | ETA Service            |
| `EtaUpdatedEvent`        | ETA Service            | Programmable Proxy     |
| `DeliveryStartedEvent`   | Delivery Service       | (informational)        |
| `DeliveryCompletedEvent` | Delivery Service       | Programmable Proxy     |
| `DeliveryFailedEvent`    | Delivery Service       | Programmable Proxy     |

All events are Java records and include `eventId`, `deliveryId`, a timestamp, and a `schemaVersion` field.

### Model (`com.fleettracking.common.model`)

- `DeliveryStatus` — `ACTIVE`, `COMPLETED`, `FAILED`
- `GeoPoint` — latitude/longitude coordinate
- `RouteDefinition` — named route with origin, destination, waypoints, and encoded polyline

### Topics (`com.fleettracking.common.topics`)

- `KafkaTopics` — constants for all Kafka topic names
- `MqttTopics` — MQTT topic pattern and builder

---

## Usage

Add as a dependency in any service `pom.xml`:

```xml
<dependency>
    <groupId>com.fleettracking</groupId>
    <artifactId>fleet-common</artifactId>
</dependency>
```

The version is managed by the parent POM.
