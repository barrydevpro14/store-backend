package org.store.contact.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.contact.domain.enums.ContactStatut;

import java.time.LocalDate;

public record ContactMessageFilter(
        String nom,
        String email,
        ContactStatut statut,
        LocalDate createdStartDate,
        LocalDate createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
