package org.store.depense.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Backing filter for {@code GET /api/v1/categories-depense}. Mirrors the rule-40 pattern adopted
 * across every CRUD list filter : optional name search (LIKE), optional actif flag, optional
 * created-at date range, mandatory pagination. The SpEL placeholders in the repository query rely
 * on the derived {@code createdStartDateTime / createdEndDateTime} accessors below.
 */
public record CategoryDepenseFilter(
        String nom,
        Boolean actif,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public LocalDateTime createdStartDateTime() {
        return createdStartDate == null ? null : createdStartDate.atStartOfDay();
    }

    public LocalDateTime createdEndDateTime() {
        return createdEndDate == null ? null : createdEndDate.plusDays(1).atStartOfDay();
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
