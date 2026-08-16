package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;

import java.math.BigDecimal;

public record AbonnementDetailsResponse(
        AbonnementResponse abonnement,
        PeriodiciteAbonnement periodicite,
        BigDecimal prix
) {
}
