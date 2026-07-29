CREATE TABLE deliveries (
    id                   UUID        PRIMARY KEY,
    vehicle_id           UUID        NOT NULL,
    status               VARCHAR(20) NOT NULL,
    origin_latitude      DOUBLE PRECISION NOT NULL,
    origin_longitude     DOUBLE PRECISION NOT NULL,
    destination_latitude DOUBLE PRECISION NOT NULL,
    destination_longitude DOUBLE PRECISION NOT NULL,
    encoded_polyline     TEXT,
    started_at           TIMESTAMPTZ,
    completed_at         TIMESTAMPTZ,
    failed_at            TIMESTAMPTZ,
    failure_reason       TEXT,
    created_at           TIMESTAMPTZ NOT NULL
);

CREATE TABLE delivery_waypoints (
    delivery_id UUID NOT NULL REFERENCES deliveries(id),
    position    INTEGER NOT NULL,
    latitude    DOUBLE PRECISION NOT NULL,
    longitude   DOUBLE PRECISION NOT NULL,
    PRIMARY KEY (delivery_id, position)
);

CREATE INDEX idx_deliveries_vehicle_id ON deliveries(vehicle_id);
CREATE INDEX idx_deliveries_status     ON deliveries(status);
