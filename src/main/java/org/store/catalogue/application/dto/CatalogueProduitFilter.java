package org.store.catalogue.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.common.validation.DatePattern;

import java.util.UUID;

public record CatalogueProduitFilter(
        UUID activiteEconomiqueId,
        String reference,
        String libelle,
        String categorie,
        @DatePattern String createdStartDate,
        @DatePattern String createdEndDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }

    public String referencePattern() {
        return (reference == null || reference.isBlank()) ? null : "%" + reference.toLowerCase() + "%";
    }

    public String libellePattern() {
        return (libelle == null || libelle.isBlank()) ? null : "%" + libelle.toLowerCase() + "%";
    }

    public String categoriePattern() {
        return (categorie == null || categorie.isBlank()) ? null : "%" + categorie.toLowerCase() + "%";
    }
}
