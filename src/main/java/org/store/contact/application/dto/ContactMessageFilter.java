package org.store.contact.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.validation.DatePattern;
import org.store.contact.domain.enums.ContactStatut;

public record ContactMessageFilter(
        String nom,
        String email,
        ContactStatut statut,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
