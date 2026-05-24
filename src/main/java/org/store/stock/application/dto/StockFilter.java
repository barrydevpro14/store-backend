package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.common.tools.DateHelper;
import org.store.common.tools.LikePatternHelper;

import java.time.LocalDateTime;
import java.util.UUID;

public record StockFilter(
        @NotNull UUID magasinId,
        UUID productId,
        String productName,
        String startDate,
        String endDate,
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

    /** Sentinel 2000-01-01 when no startDate — avoids IS NULL type-inference issue with PostgreSQL. */
    public LocalDateTime createdStart() {
        LocalDateTime parsed = DateHelper.parseStartOfDay(startDate);
        return parsed != null ? parsed : LocalDateTime.of(2000, 1, 1, 0, 0, 0);
    }

    /** Sentinel 2099-12-31 when no endDate. */
    public LocalDateTime createdEnd() {
        LocalDateTime parsed = DateHelper.parseEndOfDay(endDate);
        return parsed != null ? parsed : LocalDateTime.of(2099, 12, 31, 23, 59, 59);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "updatedAt"));
    }
}
