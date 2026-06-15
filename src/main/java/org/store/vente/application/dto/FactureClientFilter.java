package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;
import java.time.LocalDate;

import java.util.UUID;

public record FactureClientFilter(
        @NotNull UUID magasinId,
        UUID clientId,
        UUID vendeurId,
        @EnumValue(enumClass = StatutFacture.class) String statut,
        String numero,
        @DecimalMin(value = "0.0") BigDecimal montantMin,
        @DecimalMin(value = "0.0") BigDecimal montantMax,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public StatutFacture statutAsEnum() {
        return EnumHelper.parse(StatutFacture.class, statut);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
