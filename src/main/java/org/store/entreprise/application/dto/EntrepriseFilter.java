package org.store.entreprise.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public record EntrepriseFilter(
        String sigle,
        String raisonSociale,
        String ninea,
        String rccm,
        Boolean actif,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
