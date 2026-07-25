package org.store.abonnement.application.dto;

import jakarta.validation.constraints.*;
import org.store.abonnement.domain.enums.ReductionType;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;
import java.util.UUID;

public record CouponRequest(
        @NotBlank @Size(max = 100) String code,
        @Size(max = 1000) String description,
        @EnumValue(enumClass = ReductionType.class) String reductionType,
        @DecimalMin(value = "0.0", inclusive = true) BigDecimal valeurReduction,
        @PositiveOrZero int nombreUtilisationsMax,
        boolean actif,
        @NotNull UUID planId
) {
    public ReductionType reductionTypeAsEnum() {
        return reductionType == null || reductionType.isBlank() ? null : ReductionType.valueOf(reductionType);
    }
}
