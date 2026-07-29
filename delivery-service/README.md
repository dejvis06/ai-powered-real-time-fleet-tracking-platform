# Delivery Service

## Purpose

Manages the full lifecycle of a delivery: creation, start, completion, and failure. It is the authoritative owner of delivery state and writes that state to both PostgreSQL and Redis.

---

## Responsibilities

- Expose a REST API for delivery lifecycle operations
- Compute routes via Google Routes API when a delivery is created
- Persist deliveries to PostgreSQL
- Write delivery status (`ACTIVE`, `COMPLETED`, `FAILED`) to Redis on every state transition
- Publish lifecycle events to Kafka (`DELIVERY_STARTED`, `DELIVERY_COMPLETED`, `DELIVERY_FAILED`)

---

## Component Overview

```
DeliveryController          (interface/rest)
      │
      ▼
DeliveryApplicationService  (application)
      │
      ├── DeliveryRepository      → PostgreSQL
      ├── DeliveryStatePublisher  → Kafka + Redis
      └── RouteApiClient          → Google Routes API
```

The domain layer (`Delivery`, `DeliveryId`, `DeliveryDomainService`) contains all business rules and has no Spring or JPA dependency.

---

## Startup Flow

```
Spring Boot starts
      │
      ▼
JPA validates schema (Hibernate ddl-auto: validate)
      │
      ▼
Kafka producer connects
      │
      ▼
Redis connection established
      │
      ▼
REST endpoints ready on :8081
```

---

## Processing Flow

### Create Delivery

```
POST /deliveries
      │
      ▼
DeliveryController
      │
      ▼
DeliveryApplicationService.createDelivery()
      │
      ├── calls Google Routes API → gets encoded polyline + waypoints
      ├── creates Delivery aggregate (status: PENDING)
      └── saves to PostgreSQL
```

### Start Delivery

```
POST /deliveries/{id}/start
      │
      ▼
DeliveryApplicationService.startDelivery()
      │
      ├── loads Delivery from PostgreSQL
      ├── calls delivery.start()   → status: ACTIVE
      ├── saves updated state to PostgreSQL
      ├── writes "delivery:status:{id} = ACTIVE" to Redis
      └── publishes DELIVERY_STARTED to Kafka
```

### Complete / Fail Delivery

Same pattern as start — transitions to `COMPLETED` or `FAILED`, writes to Redis, publishes to Kafka.

---

## Interaction with Other Services

- **Redis**: writes `delivery:status:{deliveryId}` on every lifecycle transition. The ETA Service and Programmable Proxy read this key.
- **Kafka**: publishes `DELIVERY_STARTED`, `DELIVERY_COMPLETED`, `DELIVERY_FAILED` events.
- **Google Routes API**: called once at delivery creation to compute the route.

---

## Configuration

| Property                        | Default          | Description                    |
|---------------------------------|------------------|--------------------------------|
| `spring.datasource.url`         | localhost:5432   | PostgreSQL connection          |
| `spring.data.redis.host`        | localhost        | Redis host                     |
| `spring.kafka.bootstrap-servers`| localhost:9092   | Kafka brokers                  |
| `google.routes.api-key`         | —                | Google Routes API key (required)|
| `server.port`                   | 8081             | HTTP port                      |

---

## Running the Service

```bash
# With Maven
mvn spring-boot:run -pl delivery-service

# With Docker (after building)
docker run -p 8081:8081 \
  -e DB_HOST=localhost \
  -e REDIS_HOST=localhost \
  -e KAFKA_BOOTSTRAP_SERVERS=localhost:9092 \
  -e GOOGLE_ROUTES_API_KEY=your_key \
  fleet-delivery-service
```
