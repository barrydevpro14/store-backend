package org.store.abonnement.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;

public record SubscriptionTypeRequest(
        @NotBlank @Size(max = 255) String nom,
        @Positive int dureeMois,
        @EnumValue(enumClass = ReductionType.class) String reductionType,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal valeurReduction,
        boolean recommande,
        boolean actif,
        @PositiveOrZero int ordre
) {
    public ReductionType reductionTypeAsEnum() {
        return reductionType == null || reductionType.isBlank() ? null : ReductionType.valueOf(reductionType);
    }
}
