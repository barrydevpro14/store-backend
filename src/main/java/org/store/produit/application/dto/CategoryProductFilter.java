package org.store.produit.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record CategoryProductFilter(
        String libelle,
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
