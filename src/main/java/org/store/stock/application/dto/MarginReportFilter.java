package org.store.stock.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDateTime;
import java.util.UUID;

public record MarginReportFilter(
        @NotNull UUID magasinId,
        UUID productId,
        UUID fournisseurId,
        @DatePattern String startDate,
        @DatePattern String endDate
) {
    public LocalDateTime fromDateTime() {
        return DateHelper.parseStartOfDay(startDate);
    }

    public LocalDateTime toDateTime() {
        return DateHelper.parseEndOfDay(endDate);
    }
}
