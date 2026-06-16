package org.store.stock.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.common.validation.DatePattern;

import java.util.UUID;

public record MarginReportFilter(
        @NotNull UUID magasinId,
        UUID productId,
        UUID fournisseurId,
        @DatePattern String startDate,
        @DatePattern String endDate
) {
}
