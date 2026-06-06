package org.store.contact.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.DateHelper;
import org.store.contact.domain.enums.ContactStatut;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record ContactMessageFilter(
        String nom,
        String email,
        ContactStatut statut,
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
