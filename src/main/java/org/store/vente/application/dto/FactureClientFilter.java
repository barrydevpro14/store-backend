package org.store.vente.application.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.tools.DateHelper;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
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
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public StatutFacture statutAsEnum() {
        return EnumHelper.parse(StatutFacture.class, statut);
    }

    public LocalDateTime fromDateTime() {
        return DateHelper.parseStartOfDay(startDate);
    }

    public LocalDateTime toDateTime() {
        return DateHelper.parseEndOfDay(endDate);
    }

    public LocalDateTime createdStartDateTime() {
        return createdStartDate == null ? DateHelper.SENTINEL_START : createdStartDate.atStartOfDay();
    }

    public LocalDateTime createdEndDateTime() {
        return createdEndDate == null ? DateHelper.SENTINEL_END : createdEndDate.plusDays(1).atStartOfDay();
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
