package org.store.reporting.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.common.tools.DateHelper;

import java.time.LocalDate;
import java.util.UUID;

public record MagasinOverviewFilter(
        @NotNull UUID magasinId,
        @NotNull LocalDate startDate,
        @NotNull LocalDate endDate
) {

    public String startDateAsString() {
        return DateHelper.format(startDate);
    }

    public String endDateAsString() {
        return DateHelper.format(endDate);
    }
}
