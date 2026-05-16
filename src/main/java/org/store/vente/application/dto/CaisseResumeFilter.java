package org.store.vente.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record CaisseResumeFilter(
        @NotNull UUID magasinId,
        @NotBlank @DatePattern String date
) {
    public LocalDate dateAsLocalDate() {
        return DateHelper.parseStartOfDay(date).toLocalDate();
    }

    public LocalDateTime startOfDay() {
        return DateHelper.parseStartOfDay(date);
    }

    public LocalDateTime endOfDay() {
        return DateHelper.parseEndOfDay(date);
    }
}
