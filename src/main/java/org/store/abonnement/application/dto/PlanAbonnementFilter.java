package org.store.abonnement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

public record PlanAbonnementFilter(
        String nom,
        Boolean actif,
        Boolean visible,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "ordre", "nom"));
    }
}
