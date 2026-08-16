package org.store.abonnement.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;

public record PlanAbonnementTarifRequest(
        @NotNull @EnumValue(enumClass = PeriodiciteAbonnement.class) String periodicite,
        @NotNull @DecimalMin(value = "0.0", inclusive = false) BigDecimal prix,
        boolean actif,
        boolean recommande,
        Integer ordre
) {
    public PeriodiciteAbonnement periodiciteAsEnum() {
        return periodicite == null ? null : PeriodiciteAbonnement.valueOf(periodicite);
    }
}
