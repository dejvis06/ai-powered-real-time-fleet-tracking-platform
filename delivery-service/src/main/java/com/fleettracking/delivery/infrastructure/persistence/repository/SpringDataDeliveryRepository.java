package com.fleettracking.delivery.infrastructure.persistence.repository;

import com.fleettracking.delivery.infrastructure.persistence.entity.DeliveryJpaEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface SpringDataDeliveryRepository extends JpaRepository<DeliveryJpaEntity, UUID> {
}
