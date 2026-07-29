package com.fleettracking.eta.domain.service;

import com.fleettracking.eta.domain.model.EtaCalculation;

import java.util.UUID;

/**
 * Domain service interface for ETA calculation logic.
 */
public interface EtaCalculationService {

    /**
     * Calculates ETA for a vehicle currently at the given coordinates heading to the destination.
     */
    EtaCalculation calculate(
            UUID deliveryId,
            UUID vehicleId,
            double currentLat,
            double currentLon,
            double destLat,
            double destLon
    );
}
