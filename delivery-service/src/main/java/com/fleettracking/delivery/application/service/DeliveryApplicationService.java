package com.fleettracking.delivery.application.service;

import com.fleettracking.delivery.application.dto.CreateDeliveryRequest;
import com.fleettracking.delivery.application.dto.DeliveryResponse;
import com.fleettracking.delivery.application.dto.FailDeliveryRequest;
import com.fleettracking.delivery.application.port.DeliveryEventEmitter;
import com.fleettracking.delivery.application.port.DeliveryStatePublisher;
import com.fleettracking.delivery.application.port.RouteApiClient;
import com.fleettracking.delivery.domain.model.Delivery;
import com.fleettracking.delivery.domain.model.DeliveryId;
import com.fleettracking.delivery.domain.model.GeoPoint;
import com.fleettracking.delivery.domain.model.VehicleId;
import com.fleettracking.delivery.domain.repository.DeliveryRepository;
import com.fleettracking.delivery.domain.service.DeliveryDomainService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class DeliveryApplicationService {

    private final DeliveryRepository deliveryRepository;
    private final DeliveryDomainService domainService;
    private final DeliveryStatePublisher statePublisher;
    private final RouteApiClient routeApiClient;
    private final DeliveryEventEmitter eventEmitter;

    public DeliveryApplicationService(
            DeliveryRepository deliveryRepository,
            DeliveryDomainService domainService,
            DeliveryStatePublisher statePublisher,
            RouteApiClient routeApiClient,
            DeliveryEventEmitter eventEmitter
    ) {
        this.deliveryRepository = deliveryRepository;
        this.domainService = domainService;
        this.statePublisher = statePublisher;
        this.routeApiClient = routeApiClient;
        this.eventEmitter = eventEmitter;
    }

    @Transactional
    public DeliveryResponse createDelivery(CreateDeliveryRequest request) {
        GeoPoint origin = new GeoPoint(request.originLatitude(), request.originLongitude());
        GeoPoint destination = new GeoPoint(request.destinationLatitude(), request.destinationLongitude());

        List<GeoPoint> waypoints = request.waypoints() == null ? List.of() :
                request.waypoints().stream()
                        .map(w -> new GeoPoint(w.latitude(), w.longitude()))
                        .toList();

        RouteApiClient.RouteResult route = routeApiClient.computeRoute(origin, destination, waypoints);

        Delivery delivery = Delivery.create(
                VehicleId.of(request.vehicleId()),
                origin,
                destination,
                route.decodedWaypoints(),
                route.encodedPolyline()
        );

        deliveryRepository.save(delivery);
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse startDelivery(UUID deliveryId) {
        Delivery delivery = findOrThrow(deliveryId);
        domainService.validateStart(delivery);
        delivery.start();
        deliveryRepository.save(delivery);
        statePublisher.publishStarted(delivery);
        eventEmitter.emitStarted(deliveryId);
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse completeDelivery(UUID deliveryId) {
        Delivery delivery = findOrThrow(deliveryId);
        delivery.complete();
        deliveryRepository.save(delivery);
        statePublisher.publishCompleted(delivery);
        eventEmitter.emitCompleted(deliveryId);
        return toResponse(delivery);
    }

    @Transactional
    public DeliveryResponse failDelivery(UUID deliveryId, FailDeliveryRequest request) {
        Delivery delivery = findOrThrow(deliveryId);
        delivery.fail(request.reason());
        deliveryRepository.save(delivery);
        statePublisher.publishFailed(delivery);
        eventEmitter.emitFailed(deliveryId);
        return toResponse(delivery);
    }

    @Transactional(readOnly = true)
    public DeliveryResponse getDelivery(UUID deliveryId) {
        return toResponse(findOrThrow(deliveryId));
    }

    private Delivery findOrThrow(UUID deliveryId) {
        return deliveryRepository.findById(DeliveryId.of(deliveryId))
                .orElseThrow(() -> new DeliveryNotFoundException(deliveryId));
    }

    private DeliveryResponse toResponse(Delivery delivery) {
        List<DeliveryResponse.WaypointDto> waypoints = delivery.getRouteWaypoints().stream()
                .map(p -> new DeliveryResponse.WaypointDto(p.latitude(), p.longitude()))
                .toList();

        return new DeliveryResponse(
                delivery.getId().value(),
                delivery.getVehicleId().value(),
                delivery.getStatus(),
                delivery.getOrigin().latitude(),
                delivery.getOrigin().longitude(),
                delivery.getDestination().latitude(),
                delivery.getDestination().longitude(),
                waypoints,
                delivery.getEncodedPolyline(),
                delivery.getStartedAt(),
                delivery.getCompletedAt(),
                delivery.getFailedAt(),
                delivery.getFailureReason(),
                delivery.getCreatedAt()
        );
    }
}
