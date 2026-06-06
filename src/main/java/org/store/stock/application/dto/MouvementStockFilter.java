package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;
import org.store.stock.domain.enums.MouvementStockType;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record MouvementStockFilter(
        @NotNull UUID magasinId,
        UUID productId,
        UUID stockId,
        @EnumValue(enumClass = MouvementStockType.class) String type,
        @DatePattern String startDate,
        @DatePattern String endDate,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {

    public MouvementStockType typeAsEnum() {
        return EnumHelper.parse(MouvementStockType.class, type);
    }

    public LocalDateTime fromDateTime() {
        return DateHelper.parseStartOfDay(startDate);
    }

    public LocalDateTime toDateTime() {
        return DateHelper.parseEndOfDay(endDate);
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
