package org.store.vente.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.tools.DateHelper;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;

import java.time.LocalDateTime;
import java.util.UUID;

public record FactureClientFilter(
        @NotNull UUID magasinId,
        UUID clientId,
        @EnumValue(enumClass = StatutFacture.class) String statut,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public StatutFacture statutAsEnum() {
        return EnumHelper.parse(StatutFacture.class, statut);
    }

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
