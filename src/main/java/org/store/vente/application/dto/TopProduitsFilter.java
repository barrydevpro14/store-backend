package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.UUID;

public record TopProduitsFilter(
        @NotNull UUID magasinId,
        @DatePattern String date,
        @Min(1) int nombre
) {
    /** Date effective : la date saisie si non null/blank, sinon today(). */
    public LocalDate effectiveDate() {
        LocalDateTime parsed = DateHelper.parseStartOfDay(date);
        return parsed != null ? parsed.toLocalDate() : LocalDate.now();
    }

    public LocalDateTime startOfDay() {
        return effectiveDate().atStartOfDay();
    }

    public LocalDateTime endOfDay() {
        return effectiveDate().atTime(LocalTime.MAX);
    }

    public Pageable toPageable() {
        return PageRequest.of(0, nombre);
    }
}
