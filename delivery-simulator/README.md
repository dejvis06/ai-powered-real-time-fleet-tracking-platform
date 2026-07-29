# Delivery Simulator

## Purpose

Simulates a vehicle completing a delivery. It acts as a test driver for the full platform — calling the delivery-service REST API to manage lifecycle transitions and publishing MQTT location updates at regular intervals as it walks through the route waypoints.

---

## Responsibilities

- Create a delivery via `POST /deliveries` and store the route waypoints
- Start the delivery via `POST /deliveries/{id}/start`
- Walk through the route, publishing one MQTT location message every N seconds
- Complete the delivery via `POST /deliveries/{id}/complete` when all waypoints are visited
- Expose a `POST /simulate` endpoint to trigger a simulation programmatically

---

## Component Overview

```
SimulatorController          (interfaces)
      │
      ▼
SimulationService            (application)
      │
      ├── HttpDeliveryServiceClient  → delivery-service REST API
      └── MqttLocationPublisher      → MQTT Broker
```

---

## Startup Flow

```
Spring Boot starts
      │
      ▼
MQTT client connects to broker
      │
      ▼
Scheduled tick task enabled
      │
      ▼
REST client ready (RestClient → delivery-service)
      │
      ▼
Simulation control endpoint ready on :8086
```

---

## Processing Flow

```
POST /simulate
      │
      ▼
SimulationService.startSimulation()
      │
      ├── POST /deliveries → create delivery, get waypoints
      ├── POST /deliveries/{id}/start → ACTIVE
      └── registers simulation as RUNNING
            │
            ▼
     Scheduled tick (every N seconds)
            │
            ▼
     MqttLocationPublisher.publishLocation()
     (topic: fleet/vehicles/{vehicleId}/location)
            │
     advance to next waypoint
            │
     [when all waypoints visited]
            │
            ▼
     POST /deliveries/{id}/complete
```

---

## Interaction with Other Services

- **delivery-service**: creates and transitions deliveries via REST.
- **MQTT Broker**: publishes vehicle location messages consumed by the MQTT Ingestion Service.

---

## Configuration

| Property                          | Default                      | Description                         |
|-----------------------------------|------------------------------|-------------------------------------|
| `simulator.delivery-service-url`  | http://localhost:8081        | delivery-service base URL           |
| `simulator.location-publish-interval-ms` | 3000              | Milliseconds between location updates |
| `simulator.speed-kph`             | 50.0                         | Simulated vehicle speed             |
| `mqtt.broker-url`                 | tcp://localhost:1883         | MQTT broker                         |

---

## Running the Service

```bash
mvn spring-boot:run -pl delivery-simulator
```

Then trigger a simulation:

```bash
curl -X POST http://localhost:8086/simulate \
  -H "Content-Type: application/json" \
  -d '{"originLat":41.4147,"originLon":19.7206,"destLat":41.3275,"destLon":19.8187}'
```
