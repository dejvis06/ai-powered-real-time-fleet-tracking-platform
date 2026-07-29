# Programmable Proxy

## Purpose

Acts as the single SSE gateway for browser clients. It subscribes to SSE streams from both the ETA Service and the Delivery Service, merges them into one stream per delivery, and filters out stale events using delivery state from Redis.

---

## Responsibilities

- Accept browser SSE connections at `GET /proxy/deliveries/{id}/stream`
- Subscribe to `eta-service` SSE for `ETA_UPDATED` events
- Subscribe to `delivery-service` SSE for `DELIVERY_COMPLETED` and `DELIVERY_FAILED` events
- Merge both upstream streams into a single outbound stream
- Read Redis before relaying `ETA_UPDATED`: drop the event if the delivery is no longer `ACTIVE`
- Terminate the outbound stream when a terminal event (`DELIVERY_COMPLETED` or `DELIVERY_FAILED`) is received
- Reconnect automatically to upstream services if the connection drops

---

## Component Overview

```
Browser
      │  EventSource
      ▼
ProxyController              (interfaces/rest)
      │
      ▼
ProxyStreamService           (application)
      │
      ├── UpstreamSseClient    → subscribes to eta-service + delivery-service
      └── DeliveryStateReader  → reads delivery status from Redis
```

---

## Startup Flow

```
Spring Boot starts (Reactor Netty)
      │
      ▼
Redis reactive connection established
      │
      ▼
WebClient instances ready (eta-service, delivery-service)
      │
      ▼
SSE proxy endpoint ready on :8084
```

---

## Processing Flow

```
Browser opens EventSource to /proxy/deliveries/{id}/stream
      │
      ▼
ProxyStreamService.streamForDelivery(deliveryId)
      │
      ├── Flux A: subscribe to eta-service SSE (with retry/backoff)
      │     └── for each ETA_UPDATED:
      │           └── Redis lookup: ACTIVE? → relay / drop
      │
      ├── Flux B: subscribe to delivery-service SSE (with retry/backoff)
      │     └── relay DELIVERY_COMPLETED, DELIVERY_FAILED as-is
      │
      └── Flux.merge(A, B)
            │
            └── takeUntil DELIVERY_COMPLETED or DELIVERY_FAILED
                  │
                  ▼
            Stream terminates, browser EventSource closes
```

---

## Event Filtering Logic

| Incoming event       | Redis state | Action   |
|----------------------|-------------|----------|
| `ETA_UPDATED`        | `ACTIVE`    | Relay    |
| `ETA_UPDATED`        | `COMPLETED` | Drop     |
| `ETA_UPDATED`        | `FAILED`    | Drop     |
| `DELIVERY_COMPLETED` | any         | Relay + terminate stream |
| `DELIVERY_FAILED`    | any         | Relay + terminate stream |

---

## Interaction with Other Services

- **ETA Service**: subscribes to `GET /deliveries/{id}/sse`.
- **Delivery Service**: subscribes to `GET /deliveries/{id}/sse`.
- **Redis**: reads `delivery:status:{deliveryId}` before each `ETA_UPDATED` relay.
- **Browser**: serves the merged SSE stream.

---

## Configuration

| Property                         | Default                   | Description                          |
|----------------------------------|---------------------------|--------------------------------------|
| `proxy.eta-service-url`          | http://localhost:8083     | ETA Service base URL                 |
| `proxy.delivery-service-url`     | http://localhost:8081     | Delivery Service base URL            |
| `spring.data.redis.host`         | localhost                 | Redis host                           |
| `server.port`                    | 8084                      | HTTP port                            |

---

## Running the Service

```bash
mvn spring-boot:run -pl programmable-proxy
```
