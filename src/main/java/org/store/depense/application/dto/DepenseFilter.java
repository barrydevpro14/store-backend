package org.store.depense.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.util.UUID;

public record DepenseFilter(
        @NotNull UUID magasinId,
        UUID categoryId,
        UUID moyenPaiementId,
        String libelle,
        @DatePattern String startDate,
        @DatePattern String endDate,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public String libellePattern() {
        return (libelle == null || libelle.isBlank()) ? null : "%" + libelle.toLowerCase() + "%";
    }
    public LocalDate fromDate() {
        return DateHelper.parseStartOfDay(startDate) != null
                ? DateHelper.parseStartOfDay(startDate).toLocalDate() : null;
    }

    public LocalDate toDate() {
        return DateHelper.parseEndOfDay(endDate) != null
                ? DateHelper.parseEndOfDay(endDate).toLocalDate() : null;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
