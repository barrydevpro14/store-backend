package org.store.abonnement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.common.validation.EnumValue;

import org.store.common.tools.DateHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record PaiementAbonnementFilter(
        @EnumValue(enumClass = StatutPaiementAbonnement.class) String statut,
        UUID abonnementId,
        UUID entrepriseId,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public StatutPaiementAbonnement statutAsEnum() {
        return statut == null || statut.isBlank() ? null : StatutPaiementAbonnement.valueOf(statut);
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
