package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record StockFilter(
        @NotNull UUID magasinId,
        UUID productId,
        String productName,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    /** Backward-compatible compact constructor for existing call sites. */
    public StockFilter(UUID magasinId, UUID productId, int page, int size) {
        this(magasinId, productId, null, null, null, page, size);
    }

    /** Pre-built LIKE pattern; null when productName is blank → query skips this filter. */
    public String productNamePattern() {
        return LikePatternHelper.toLikePattern(productName);
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
