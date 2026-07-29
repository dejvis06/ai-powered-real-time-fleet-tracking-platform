package com.fleettracking.common.topics;

public final class KafkaTopics {

    public static final String VEHICLE_LOCATION = "vehicle-location";
    public static final String ETA_UPDATED = "eta-updated";
    public static final String DELIVERY_STARTED = "delivery-started";
    public static final String DELIVERY_COMPLETED = "delivery-completed";
    public static final String DELIVERY_FAILED = "delivery-failed";

    private KafkaTopics() {
    }
}
