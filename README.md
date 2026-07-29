# AI-Powered Real-Time Fleet Tracking Platform

A distributed, event-driven system for tracking delivery vehicles in real time. The platform combines MQTT for vehicle telemetry, Apache Kafka for reliable event streaming, Google Routes API for live ETA calculation, and Server-Sent Events (SSE) for push updates to browser clients.

---

## Architecture Overview

### 1. Delivery Lifecycle & Location Ingestion

A simulated vehicle (or real GPS device) uses two separate channels:

```
DELIVERER
(Simulator / Vehicle)
│
├── REST: start delivery
├── REST: end delivery
│     │
│     ▼
│   Delivery Service
│     │
│     └── publishes
│           DELIVERY_STARTED /
│           DELIVERY_COMPLETED
│           to Kafka
│
└── MQTT: vehicle location
      │
      ▼
    MQTT Broker
      │
      ▼
    MQTT Ingestion Service
      │
      └── publishes
            VEHICLE_LOCATION
            to Kafka
```

The deliverer uses **REST for delivery lifecycle** and **MQTT for continuous location updates**.

---

### 2. ETA Calculation Flow

```
CUSTOMER
(Web Dashboard +
 Google Maps)
│
SSE / WebSocket
│
▼
Reverse Proxy
│
▼
ETA Service Instance
(Kafka partition owner)
▲
│
VEHICLE_LOCATION
│
┌────┴────┐
│  Kafka  │
└────▲────┘
     │
VEHICLE_LOCATION
     │
MQTT Ingestion Service
     ▲
     │
 MQTT Broker
     ▲
     │
MQTT Client
     ▲
     │
Deliverer (Simulator / Vehicle)
```

Each ETA Service instance owns one or more Kafka partitions (partitioned by `deliveryId`), ensuring all location events for a given delivery are processed by the same instance.

---

### 3. ETA Service Scalability

The platform configures **4 Kafka partitions** and supports **up to 8 ETA service instances**:

```
Kafka Topic (partitioned by deliveryId)
│
┌──────────┬──────────┬──────────┬──────────┐
▼          ▼          ▼          ▼
Partition 0  Partition 1  Partition 2  Partition 3
│            │            │            │
▼            ▼            ▼            ▼
ETA Inst A   ETA Inst B   ETA Inst C   ETA Inst D
(active)     (active)     (active)     (active)

─────── Same Consumer Group ───────

ETA Inst E   ETA Inst F   ETA Inst G   ETA Inst H
(idle)       (idle)       (idle)       (idle)
```

When an instance crashes, Kafka rebalances — an idle instance takes over the affected partition automatically.

---

### 4. Programmable Reverse Proxy

The proxy subscribes to SSE streams from both the ETA Service and the Delivery Service, merges them, and forwards a single unified stream to the browser client:

```
Client
(Google Maps UI)
▲
│
Single SSE stream
│
┌─────────────────────┴─────────────────────┐
│         Programmable Reverse Proxy         │
│                                            │
│  Incoming SSE events                       │
│  ──────────────────                        │
│  ETA_UPDATED      ◄── ETA Service          │
│  DELIVERY_COMPLETED ◄── Delivery Service   │
│  DELIVERY_FAILED  ◄── Delivery Service     │
│                                            │
│  Stream Aggregator                         │
│  (merge + forward events)                  │
│                                            │
│  Outgoing SSE stream                       │
│  ──────────────────                        │
│  ETA_UPDATED                               │
│  DELIVERY_COMPLETED                        │
│  DELIVERY_FAILED                           │
└─────────────────────▲─────────────────────┘
                       │
               SSE subscriptions
                ▲          ▲
                │          │
          ETA Service  Delivery Service
```

---

### 5. Proxy Thread Model

Each proxy instance uses Reactor Netty's event-loop threading. A single event-loop thread handles many concurrent SSE connections:

```
Proxy Instance A
┌──────────────────────────────┐
│         Event Loop 1         │
└──────────────────────────────┘
    │         │         │
    ▼         ▼         ▼
SSE #1     SSE #2     SSE #3
Browser   Browser   Browser

┌──────────────────────────────┐
│         Event Loop 2         │
└──────────────────────────────┘
    │         │         │
    ▼         ▼         ▼
SSE #4     SSE #5     SSE #6
```

---

### 6. State Management & Redis

The proxy acts as a **filter** using delivery state stored in Redis:

- `ETA_UPDATED` → relayed only while delivery is **ACTIVE**
- `DELIVERY_COMPLETED` / `DELIVERY_FAILED` → update state, forward to client
- Any subsequent `ETA_UPDATED` for a completed/failed delivery → **dropped**

```
              Redis
       deliveryId → ACTIVE
       deliveryId → COMPLETED
       deliveryId → FAILED
              ▲
    read │       │ write
              │
 ┌────────────┼────────────┐
 │            │            │
 ▼            ▼            ▼
ETA Service  Delivery    Programmable
             Service     Reverse Proxy
│            │           │
│ Read state │ Write     │ Read state
│ from Redis │ ACTIVE    │ before relaying
│            │ COMPLETED │
│ Calculate  │ FAILED    │ Relay / Drop
│ only if    │           │
│ ACTIVE     │           │
▼            ▼           ▼
ETA_UPDATED  DELIVERY_*  Client (SSE)
             events
```

**Redis is the single source of truth for delivery state.**

---

## Repository Structure

```
fleet-tracking-platform/
├── pom.xml                        # Parent Maven POM (Spring Boot 4.1.0, Java 25)
├── fleet-common/                  # Shared events, models, topic constants
├── delivery-service/              # Spring MVC + Jetty — REST API, PostgreSQL, Kafka publisher
├── delivery-simulator/            # Non-web — REST client + MQTT publisher
├── mqtt-ingestion-service/        # Non-web — MQTT subscriber + Kafka producer
├── eta-service/                   # Spring WebFlux — Kafka consumer, Routes API, SSE source
├── programmable-proxy/            # Spring WebFlux — SSE aggregator, Redis filter
├── fleet-web-client/              # Plain JS — Google Maps + EventSource
└── infrastructure/
    ├── docker-compose.yml
    ├── mosquitto/
    │   └── mosquitto.conf
    └── kafka/
        └── create-topics.sh
```

---

## Technology Stack

| Component              | Version / Notes                              |
|------------------------|----------------------------------------------|
| Java                   | 25 LTS                                       |
| Maven                  | 3.9.x                                        |
| Spring Boot            | 4.1.0                                        |
| Spring WebFlux         | Boot-managed                                 |
| Spring Kafka           | Boot-managed (4.1.0 reference)               |
| Spring Data Redis      | Boot-managed                                 |
| Apache Kafka           | 4.3.1 (KRaft mode — no ZooKeeper)            |
| Eclipse Mosquitto      | 2.1.2                                        |
| Redis Open Source      | 8.6.x                                        |
| PostgreSQL             | 18.x                                         |
| Google Maps JS API     | Quarterly channel                            |
| Google Routes API      | REST v2                                      |

---

## Service Port Map

| Service                | Port  |
|------------------------|-------|
| delivery-service       | 8081  |
| eta-service            | 8083  |
| programmable-proxy     | 8084  |
| mqtt-ingestion-service | 8085  |
| delivery-simulator     | 8086  |
| PostgreSQL             | 5432  |
| Redis                  | 6379  |
| Kafka                  | 9092  |
| Mosquitto (MQTT)       | 1883  |
| Mosquitto (WS)         | 9001  |

---

## Quick Start

### Prerequisites

- Docker + Docker Compose
- Google Maps API key with Routes API enabled

### Steps

```bash
# 1. Clone the repository
git clone <repo-url>
cd fleet-tracking-platform

# 2. Set your Google API key
cp .env.example .env
# Edit .env and set GOOGLE_ROUTES_API_KEY

# 3. Build all services
mvn clean package -DskipTests

# 4. Start the infrastructure stack
cd infrastructure
docker compose up -d

# 5. Open the web client
open fleet-web-client/index.html
# Add your Google Maps API key in index.html first

# 6. Trigger a simulation (example coordinates: Tirana airport → city center)
curl -X POST http://localhost:8086/simulate \
  -H "Content-Type: application/json" \
  -d '{"originLat":41.4147,"originLon":19.7206,"destLat":41.3275,"destLon":19.8187}'

# 7. Paste the returned deliveryId into the web client and click "Track Delivery"
```

---

## Observability

Every backend service exposes:

- `GET /actuator/health`
- `GET /actuator/info`
- `GET /actuator/prometheus`

Key metrics tracked across the platform:

| Metric                      | Description                              |
|-----------------------------|------------------------------------------|
| `mqtt.messages.received`    | MQTT messages successfully ingested      |
| `mqtt.messages.rejected`    | MQTT messages that failed processing     |
| `kafka.events.published`    | Events sent to Kafka                     |
| `kafka.events.failed`       | Kafka publish failures                   |
| `eta.calculations`          | ETA calculations performed               |
| `eta.calculation.duration`  | Latency of ETA calculation               |
| `google.routes.requests`    | Google Routes API calls                  |
| `google.routes.errors`      | Google Routes API errors                 |
| `google.routes.latency`     | Google Routes API latency                |
| `sse.connections.active`    | Active SSE connections at the proxy      |
| `sse.events.forwarded`      | Events relayed to clients                |
| `sse.events.dropped`        | Events dropped (stale after completion)  |
| `redis.routing.lookup`      | Redis state cache lookups                |
| `redis.routing.miss`        | Redis cache misses                       |

---

## Build, Start & Use

### Prerequisites

- Java 25
- Maven 3.9+
- Docker + Docker Compose
- A Google Cloud project with both the **Maps JavaScript API** and the **Routes API** enabled
- A Google API key for the Routes API (backend) and optionally a separate browser-restricted key for the Maps JS API

---

### 1. Configure secrets

```bash
cp .env.example .env
```

Open `.env` and set your key:

```
GOOGLE_ROUTES_API_KEY=your_key_here
```

Also open `fleet-web-client/index.html` and replace `YOUR_GOOGLE_MAPS_API_KEY` in the `<script>` tag at the bottom with your Maps JavaScript API key.

---

### 2. Build all services

Run this from the repository root. It compiles every module and produces a fat JAR in each service's `target/` folder.

```bash
mvn clean package -DskipTests
```

---

### 3. Start the infrastructure and services

```bash
cd infrastructure
docker compose up -d
```

Docker Compose will start (in dependency order):

1. PostgreSQL, Redis, Mosquitto, Kafka
2. A one-shot `kafka-init` container that creates all required topics
3. delivery-service, mqtt-ingestion-service, eta-service, programmable-proxy, delivery-simulator

Wait ~30 seconds for everything to be healthy. You can check with:

```bash
docker compose ps
```

All services should show `healthy` or `running`.

---

### 4. Open the web client

Open `fleet-web-client/index.html` directly in your browser (no server needed — it is a static file).

You should see a map centered on Tirana with an empty sidebar on the left.

---

### 5. Start a delivery simulation

Send a POST request to the simulator with origin and destination coordinates. This example routes from Tirana International Airport to the city center:

```bash
curl -X POST http://localhost:8086/simulate \
  -H "Content-Type: application/json" \
  -d '{
    "originLat": 41.4147,
    "originLon": 19.7206,
    "destLat":   41.3275,
    "destLon":   19.8187
  }'
```

The response contains the `deliveryId`:

```json
{ "deliveryId": "e3f1a2b4-..." }
```

Behind the scenes the simulator:
1. calls `POST /deliveries` on the delivery-service → Google Routes API computes the route
2. calls `POST /deliveries/{id}/start` → status written to Redis as `ACTIVE`, event published to Kafka
3. publishes a vehicle location to MQTT every 3 seconds as it steps through waypoints

---

### 6. Track the delivery in the browser

1. Paste the `deliveryId` into the **Delivery ID** field in the web client.
2. Make sure the **Proxy URL** field is `http://localhost:8084` (the default).
3. Click **Track Delivery**.

The browser opens an EventSource connection to the programmable proxy. As the simulator moves the vehicle:

- The map pans and the vehicle marker animates along the route.
- The sidebar updates with the current ETA and remaining distance.
- When the simulator reaches the destination and calls `complete`, the stream closes and the sidebar shows **COMPLETED**.

---

### 7. Stop everything

```bash
cd infrastructure
docker compose down
```

Add `-v` to also remove the PostgreSQL, Redis, and Kafka volumes:

```bash
docker compose down -v
```

---

### Scaling the ETA Service

The platform is designed for 4 Kafka partitions and up to 8 ETA Service instances. To scale locally:

```bash
docker compose up -d --scale eta-service=4
```

Kafka will rebalance partitions across the new instances automatically. If one instance goes down, Kafka reassigns its partition to a standby within seconds.
