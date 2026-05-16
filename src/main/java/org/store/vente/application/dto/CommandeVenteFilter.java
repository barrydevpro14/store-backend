package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDateTime;
import java.util.UUID;

public record CommandeVenteFilter(
        @NotNull UUID magasinId,
        UUID clientId,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public LocalDateTime fromDateTime() {
        return DateHelper.parseStartOfDay(startDate);
    }

    public LocalDateTime toDateTime() {
        return DateHelper.parseEndOfDay(endDate);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
