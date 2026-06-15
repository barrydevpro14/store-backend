package org.store.stock.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;
import org.store.stock.domain.enums.MouvementStockType;

import java.util.UUID;

public record MouvementStockFilter(
        @NotNull UUID magasinId,
        UUID productId,
        UUID stockId,
        @EnumValue(enumClass = MouvementStockType.class) String type,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public MouvementStockType typeAsEnum() {
        return EnumHelper.parse(MouvementStockType.class, type);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
