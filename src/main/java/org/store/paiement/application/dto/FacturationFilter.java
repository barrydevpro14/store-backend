package org.store.paiement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record FacturationFilter(
        UUID moyenPaiementId,
        UUID paysId,
        Boolean actif,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }

    public LocalDateTime createdStartDateTime() {
        return createdStartDate != null ? createdStartDate.atStartOfDay() : null;
    }

    public LocalDateTime createdEndDateTime() {
        return createdEndDate != null ? createdEndDate.plusDays(1).atStartOfDay() : null;
    }
}
