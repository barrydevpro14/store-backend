package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.math.BigDecimal;
import java.util.UUID;

public record PlanAbonnementTarifResponse(
        UUID id,
        PeriodiciteAbonnement periodicite,
        BigDecimal prix,
        boolean recommande,
        Integer ordre
) {
    public PlanAbonnementTarifResponse(PlanAbonnementTarif tarif) {
        this(tarif.getId(), tarif.getPeriodicite(), tarif.getPrix(), tarif.isRecommande(), tarif.getOrdre());
    }
}
