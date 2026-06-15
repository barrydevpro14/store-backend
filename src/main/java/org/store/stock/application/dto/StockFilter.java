package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.LikePatternHelper;

import java.time.LocalDate;
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

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
