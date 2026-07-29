package com.fleettracking.common.topics;

public final class MqttTopics {

    /**
     * MQTT topic for vehicle location updates.
     * Pattern: fleet/vehicles/{vehicleId}/location
     */
    public static final String VEHICLE_LOCATION = "fleet/vehicles/+/location";

    /**
     * Builds a specific vehicle location topic.
     */
    public static String vehicleLocation(String vehicleId) {
        return "fleet/vehicles/" + vehicleId + "/location";
    }

    private MqttTopics() {
    }
}
