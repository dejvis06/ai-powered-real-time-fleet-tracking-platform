package com.fleettracking.simulator.domain.model;

import java.util.List;
import java.util.UUID;

/**
 * Domain model representing an in-progress simulated delivery.
 */
public class SimulatedDelivery {

    private final UUID deliveryId;
    private final UUID vehicleId;
    private final List<Waypoint> route;
    private int currentWaypointIndex;
    private SimulationStatus status;

    public SimulatedDelivery(UUID deliveryId, UUID vehicleId, List<Waypoint> route) {
        this.deliveryId = deliveryId;
        this.vehicleId = vehicleId;
        this.route = List.copyOf(route);
        this.currentWaypointIndex = 0;
        this.status = SimulationStatus.PENDING;
    }

    public boolean hasMoreWaypoints() {
        return currentWaypointIndex < route.size();
    }

    public Waypoint currentWaypoint() {
        if (!hasMoreWaypoints()) {
            throw new IllegalStateException("No more waypoints for delivery " + deliveryId);
        }
        return route.get(currentWaypointIndex);
    }

    public void advanceToNextWaypoint() {
        currentWaypointIndex++;
    }

    public double headingToNext() {
        if (currentWaypointIndex + 1 >= route.size()) return 0.0;
        Waypoint current = route.get(currentWaypointIndex);
        Waypoint next = route.get(currentWaypointIndex + 1);
        return calculateBearing(current.latitude(), current.longitude(),
                next.latitude(), next.longitude());
    }

    private double calculateBearing(double lat1, double lon1, double lat2, double lon2) {
        double dLon = Math.toRadians(lon2 - lon1);
        double y = Math.sin(dLon) * Math.cos(Math.toRadians(lat2));
        double x = Math.cos(Math.toRadians(lat1)) * Math.sin(Math.toRadians(lat2))
                - Math.sin(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) * Math.cos(dLon);
        return (Math.toDegrees(Math.atan2(y, x)) + 360) % 360;
    }

    public UUID getDeliveryId() { return deliveryId; }
    public UUID getVehicleId() { return vehicleId; }
    public List<Waypoint> getRoute() { return route; }
    public SimulationStatus getStatus() { return status; }
    public void setStatus(SimulationStatus status) { this.status = status; }

    public record Waypoint(double latitude, double longitude) {}

    public enum SimulationStatus {
        PENDING, RUNNING, COMPLETED, FAILED
    }
}
