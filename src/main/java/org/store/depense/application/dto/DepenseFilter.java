package org.store.depense.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.common.validation.DatePattern;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

public record DepenseFilter(
        @NotNull UUID magasinId,
        UUID categoryId,
        UUID moyenPaiementId,
        @DatePattern String startDate,
        @DatePattern String endDate,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public LocalDate fromDate() {
        return DateHelper.parseStartOfDay(startDate) != null
                ? DateHelper.parseStartOfDay(startDate).toLocalDate() : null;
    }

    public LocalDate toDate() {
        return DateHelper.parseEndOfDay(endDate) != null
                ? DateHelper.parseEndOfDay(endDate).toLocalDate() : null;
    }

    public LocalDate fromDateSentinel() {
        LocalDate d = fromDate();
        return d != null ? d : LocalDate.of(2000, 1, 1);
    }

    public LocalDate toDateSentinel() {
        LocalDate d = toDate();
        return d != null ? d : LocalDate.of(2099, 12, 31);
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
