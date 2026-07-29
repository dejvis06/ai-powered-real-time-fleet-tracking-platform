# ETA Service

## Purpose

Calculates the estimated time of arrival (ETA) for active deliveries. It consumes vehicle location events from Kafka, checks delivery state in Redis, calls the Google Routes API to compute the remaining route duration, and streams the result to the Programmable Proxy via SSE.

---

## Responsibilities

- Consume `VEHICLE_LOCATION` events from Kafka (partitioned by `deliveryId`)
- Check Redis: if the delivery is not `ACTIVE`, discard the event immediately
- Fetch the delivery destination from Redis
- Call Google Routes API to compute remaining distance and duration
- Emit `ETA_UPDATED` events to all SSE clients subscribed to that delivery
- Track `eta.calculations`, `eta.calculation.duration`, `google.routes.*` metrics

---

## Component Overview

```
VehicleLocationConsumer      (infrastructure/kafka)
      │
      ▼
EtaApplicationService        (application)
      │
      ├── DeliveryStateCache   → Redis (is delivery ACTIVE?)
      ├── RoutesApiPort        → Google Routes API
      └── SseEmitterRegistry   → broadcast ETA_UPDATED to SSE clients
            │
            ▼
      EtaSseController         (interfaces/rest)
      GET /deliveries/{id}/sse → Programmable Proxy subscribes here
```

---

## Startup Flow

```
Spring Boot starts (Reactor Netty)
      │
      ▼
Kafka consumer connects and joins consumer group "eta-service"
Kafka assigns partitions (by deliveryId)
      │
      ▼
Redis reactive connection established
      │
      ▼
SSE endpoint ready on :8083/deliveries/{id}/sse
```

---

## Processing Flow

```
Kafka partition N
      │
      ▼
VehicleLocationConsumer.consume()
      │
      ▼
EtaApplicationService.processLocationEvent()
      │
      ├── Redis: delivery:status:{id} == ACTIVE?
      │     └── NO → drop event (log + metric)
      │     └── YES →
      │
      ├── Redis: get destination lat/lon
      │
      ├── Google Routes API: computeRoute(current → destination)
      │     └── returns distanceMeters, durationSeconds
      │
      ├── build EtaUpdatedEvent
      │
      └── SseEmitterRegistry.emit(deliveryId, event)
            │
            ▼
      Programmable Proxy (SSE subscriber)
```

---

## Scalability

Each instance of this service owns a subset of Kafka partitions. Since all events for a given `deliveryId` go to the same partition (and therefore the same instance), no cross-instance coordination is needed.

Scale to 8 instances for production:

```bash
docker compose up --scale eta-service=8
```

---

## Interaction with Other Services

- **Kafka**: consumes from `vehicle-location`.
- **Redis**: reads `delivery:status:{id}` and `delivery:destination:lat/lon:{id}`.
- **Google Routes API**: called on every received location event.
- **Programmable Proxy**: subscribes to `GET /deliveries/{id}/sse`.

---

## Configuration

| Property                         | Default           | Description                    |
|----------------------------------|-------------------|--------------------------------|
| `spring.kafka.bootstrap-servers` | localhost:9092    | Kafka brokers                  |
| `spring.kafka.consumer.group-id` | eta-service       | Consumer group                 |
| `spring.data.redis.host`         | localhost         | Redis host                     |
| `google.routes.api-key`          | —                 | Google Routes API key (required)|
| `server.port`                    | 8083              | HTTP port                      |

---

## Running the Service

```bash
mvn spring-boot:run -pl eta-service
```
