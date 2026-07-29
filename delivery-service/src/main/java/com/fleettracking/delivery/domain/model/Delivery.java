package com.fleettracking.delivery.domain.model;

import java.time.Instant;
import java.util.List;

/**
 * Delivery aggregate root.
 * Owns the delivery lifecycle: PENDING → ACTIVE → COMPLETED | FAILED.
 */
public class Delivery {

    private final DeliveryId id;
    private final VehicleId vehicleId;
    private final GeoPoint origin;
    private final GeoPoint destination;
    private final List<GeoPoint> routeWaypoints;
    private final String encodedPolyline;

    private DeliveryStatus status;
    private Instant startedAt;
    private Instant completedAt;
    private Instant failedAt;
    private String failureReason;
    private final Instant createdAt;

    private Delivery(
            DeliveryId id,
            VehicleId vehicleId,
            GeoPoint origin,
            GeoPoint destination,
            List<GeoPoint> routeWaypoints,
            String encodedPolyline,
            DeliveryStatus status,
            Instant createdAt
    ) {
        this.id = id;
        this.vehicleId = vehicleId;
        this.origin = origin;
        this.destination = destination;
        this.routeWaypoints = List.copyOf(routeWaypoints);
        this.encodedPolyline = encodedPolyline;
        this.status = status;
        this.createdAt = createdAt;
    }

    /**
     * Factory: creates a new Delivery in PENDING state.
     */
    public static Delivery create(
            VehicleId vehicleId,
            GeoPoint origin,
            GeoPoint destination,
            List<GeoPoint> routeWaypoints,
            String encodedPolyline
    ) {
        return new Delivery(
                DeliveryId.generate(),
                vehicleId,
                origin,
                destination,
                routeWaypoints,
                encodedPolyline,
                DeliveryStatus.PENDING,
                Instant.now()
        );
    }

    /**
     * Reconstitution: rebuilds a Delivery from persisted state.
     */
    public static Delivery reconstitute(
            DeliveryId id,
            VehicleId vehicleId,
            GeoPoint origin,
            GeoPoint destination,
            List<GeoPoint> routeWaypoints,
            String encodedPolyline,
            DeliveryStatus status,
            Instant startedAt,
            Instant completedAt,
            Instant failedAt,
            String failureReason,
            Instant createdAt
    ) {
        Delivery delivery = new Delivery(id, vehicleId, origin, destination,
                routeWaypoints, encodedPolyline, status, createdAt);
        delivery.startedAt = startedAt;
        delivery.completedAt = completedAt;
        delivery.failedAt = failedAt;
        delivery.failureReason = failureReason;
        return delivery;
    }

    // --- Business rules ---

    public void start() {
        if (status != DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Delivery %s cannot be started from status %s".formatted(id, status));
        }
        this.status = DeliveryStatus.ACTIVE;
        this.startedAt = Instant.now();
    }

    public void complete() {
        if (status != DeliveryStatus.ACTIVE) {
            throw new IllegalStateException(
                    "Delivery %s cannot be completed from status %s".formatted(id, status));
        }
        this.status = DeliveryStatus.COMPLETED;
        this.completedAt = Instant.now();
    }

    public void fail(String reason) {
        if (status != DeliveryStatus.ACTIVE && status != DeliveryStatus.PENDING) {
            throw new IllegalStateException(
                    "Delivery %s cannot be failed from status %s".formatted(id, status));
        }
        this.status = DeliveryStatus.FAILED;
        this.failedAt = Instant.now();
        this.failureReason = reason;
    }

    // --- Getters ---

    public DeliveryId getId() { return id; }
    public VehicleId getVehicleId() { return vehicleId; }
    public GeoPoint getOrigin() { return origin; }
    public GeoPoint getDestination() { return destination; }
    public List<GeoPoint> getRouteWaypoints() { return routeWaypoints; }
    public String getEncodedPolyline() { return encodedPolyline; }
    public DeliveryStatus getStatus() { return status; }
    public Instant getStartedAt() { return startedAt; }
    public Instant getCompletedAt() { return completedAt; }
    public Instant getFailedAt() { return failedAt; }
    public String getFailureReason() { return failureReason; }
    public Instant getCreatedAt() { return createdAt; }
}
