package com.fleettracking.delivery.domain.repository;

import com.fleettracking.delivery.domain.model.Delivery;
import com.fleettracking.delivery.domain.model.DeliveryId;

import java.util.Optional;

/**
 * Domain repository interface — no Spring or JPA dependency here.
 */
public interface DeliveryRepository {

    Delivery save(Delivery delivery);

    Optional<Delivery> findById(DeliveryId id);
}
