package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.UUID;

public record ExpiringLotsFilter(
        @NotNull UUID magasinId,
        UUID productId,
        @NotNull @Min(0) Integer daysAhead,
        @Min(0) int page,
        @Min(1) int size
) {
    public LocalDate untilDate() {
        return LocalDate.now().plusDays(daysAhead);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
