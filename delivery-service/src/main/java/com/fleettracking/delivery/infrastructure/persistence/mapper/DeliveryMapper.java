package com.fleettracking.delivery.infrastructure.persistence.mapper;

import com.fleettracking.delivery.domain.model.Delivery;
import com.fleettracking.delivery.domain.model.DeliveryId;
import com.fleettracking.delivery.domain.model.GeoPoint;
import com.fleettracking.delivery.domain.model.VehicleId;
import com.fleettracking.delivery.infrastructure.persistence.entity.DeliveryJpaEntity;
import com.fleettracking.delivery.infrastructure.persistence.entity.WaypointEmbeddable;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class DeliveryMapper {

    public DeliveryJpaEntity toEntity(Delivery delivery) {
        DeliveryJpaEntity entity = new DeliveryJpaEntity();
        entity.setId(delivery.getId().value());
        entity.setVehicleId(delivery.getVehicleId().value());
        entity.setStatus(delivery.getStatus());
        entity.setOriginLatitude(delivery.getOrigin().latitude());
        entity.setOriginLongitude(delivery.getOrigin().longitude());
        entity.setDestinationLatitude(delivery.getDestination().latitude());
        entity.setDestinationLongitude(delivery.getDestination().longitude());
        entity.setWaypoints(delivery.getRouteWaypoints().stream()
                .map(p -> new WaypointEmbeddable(p.latitude(), p.longitude()))
                .toList());
        entity.setEncodedPolyline(delivery.getEncodedPolyline());
        entity.setStartedAt(delivery.getStartedAt());
        entity.setCompletedAt(delivery.getCompletedAt());
        entity.setFailedAt(delivery.getFailedAt());
        entity.setFailureReason(delivery.getFailureReason());
        entity.setCreatedAt(delivery.getCreatedAt());
        return entity;
    }

    public Delivery toDomain(DeliveryJpaEntity entity) {
        List<GeoPoint> waypoints = entity.getWaypoints() == null ? List.of() :
                entity.getWaypoints().stream()
                        .map(w -> new GeoPoint(w.getLatitude(), w.getLongitude()))
                        .toList();

        return Delivery.reconstitute(
                DeliveryId.of(entity.getId()),
                VehicleId.of(entity.getVehicleId()),
                new GeoPoint(entity.getOriginLatitude(), entity.getOriginLongitude()),
                new GeoPoint(entity.getDestinationLatitude(), entity.getDestinationLongitude()),
                waypoints,
                entity.getEncodedPolyline(),
                entity.getStatus(),
                entity.getStartedAt(),
                entity.getCompletedAt(),
                entity.getFailedAt(),
                entity.getFailureReason(),
                entity.getCreatedAt()
        );
    }
}
