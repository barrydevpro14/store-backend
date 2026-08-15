package org.store.abonnement.application.dto;

import jakarta.validation.constraints.*;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record CouponRequest(
        @NotBlank @Size(max = 100) String code,
        @Size(max = 1000) String description,
        @EnumValue(enumClass = ReductionType.class) String reductionType,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal valeurReduction,
        @PositiveOrZero int nombreUtilisationsMax,
        boolean actif,
        @EnumValue(enumClass = PeriodiciteAbonnement.class) String periodicite,
        @NotNull LocalDate dateDebut,
        @NotNull LocalDate dateFin,
        UUID planId
) {
    public ReductionType reductionTypeAsEnum() {
        return reductionType == null || reductionType.isBlank() ? null : ReductionType.valueOf(reductionType);
    }

    public PeriodiciteAbonnement periodiciteAsEnum() {
        return periodicite == null || periodicite.isBlank() ? null : PeriodiciteAbonnement.valueOf(periodicite);
    }
}
