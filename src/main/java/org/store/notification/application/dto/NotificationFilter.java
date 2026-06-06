package org.store.notification.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.notification.domain.enums.NotificationStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record NotificationFilter(
        NotificationStatut statut,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public LocalDateTime createdStartDateTime() {
        return DateHelper.coalesceStart(createdStartDate != null ? createdStartDate.atStartOfDay() : null);
    }

    public LocalDateTime createdEndDateTime() {
        return DateHelper.coalesceEnd(createdEndDate != null ? createdEndDate.plusDays(1).atStartOfDay() : null);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
