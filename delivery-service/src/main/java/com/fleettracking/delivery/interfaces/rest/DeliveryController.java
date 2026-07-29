package com.fleettracking.delivery.interfaces.rest;

import com.fleettracking.delivery.application.dto.CreateDeliveryRequest;
import com.fleettracking.delivery.application.dto.DeliveryResponse;
import com.fleettracking.delivery.application.dto.FailDeliveryRequest;
import com.fleettracking.delivery.application.service.DeliveryApplicationService;
import com.fleettracking.delivery.application.service.DeliveryNotFoundException;
import com.fleettracking.delivery.infrastructure.sse.DeliverySseRegistry;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.UUID;

@RestController
@RequestMapping("/deliveries")
public class DeliveryController {

    private final DeliveryApplicationService applicationService;
    private final DeliverySseRegistry sseRegistry;

    public DeliveryController(DeliveryApplicationService applicationService, DeliverySseRegistry sseRegistry) {
        this.applicationService = applicationService;
        this.sseRegistry = sseRegistry;
    }

    @PostMapping
    public ResponseEntity<DeliveryResponse> createDelivery(@Valid @RequestBody CreateDeliveryRequest request) {
        DeliveryResponse response = applicationService.createDelivery(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping("/{deliveryId}")
    public ResponseEntity<DeliveryResponse> getDelivery(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(applicationService.getDelivery(deliveryId));
    }

    @PostMapping("/{deliveryId}/start")
    public ResponseEntity<DeliveryResponse> startDelivery(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(applicationService.startDelivery(deliveryId));
    }

    @PostMapping("/{deliveryId}/complete")
    public ResponseEntity<DeliveryResponse> completeDelivery(@PathVariable UUID deliveryId) {
        return ResponseEntity.ok(applicationService.completeDelivery(deliveryId));
    }

    @GetMapping(value = "/{deliveryId}/sse", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamDeliveryEvents(@PathVariable UUID deliveryId) {
        return sseRegistry.subscribe(deliveryId);
    }

    @PostMapping("/{deliveryId}/fail")
    public ResponseEntity<DeliveryResponse> failDelivery(
            @PathVariable UUID deliveryId,
            @RequestBody(required = false) FailDeliveryRequest request
    ) {
        FailDeliveryRequest req = request != null ? request : new FailDeliveryRequest("Unspecified failure");
        return ResponseEntity.ok(applicationService.failDelivery(deliveryId, req));
    }

    @ExceptionHandler(DeliveryNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(DeliveryNotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(new ErrorResponse(ex.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<ErrorResponse> handleIllegalState(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(new ErrorResponse(ex.getMessage()));
    }

    public record ErrorResponse(String message) {}
}
