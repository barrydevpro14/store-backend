package org.store.depense.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.tools.DateHelper;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;

import java.time.LocalDate;
import java.util.UUID;

public record DepenseFilter(
        @NotNull UUID magasinId,
        UUID categoryId,
        @EnumValue(enumClass = MoyenPaiement.class) String modePaiement,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public MoyenPaiement modePaiementAsEnum() {
        return EnumHelper.parse(MoyenPaiement.class, modePaiement);
    }

    public LocalDate fromDate() {
        return DateHelper.parseStartOfDay(startDate) != null
                ? DateHelper.parseStartOfDay(startDate).toLocalDate() : null;
    }

    public LocalDate toDate() {
        return DateHelper.parseEndOfDay(endDate) != null
                ? DateHelper.parseEndOfDay(endDate).toLocalDate() : null;
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
