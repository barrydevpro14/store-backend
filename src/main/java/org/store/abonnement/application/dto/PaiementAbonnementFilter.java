package org.store.abonnement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.common.validation.EnumValue;

import java.util.UUID;

public record PaiementAbonnementFilter(
        @EnumValue(enumClass = StatutPaiementAbonnement.class) String statut,
        UUID abonnementId,
        UUID entrepriseId,
        @Min(0) int page,
        @Min(1) int size
) {
    public StatutPaiementAbonnement statutAsEnum() {
        return statut == null || statut.isBlank() ? null : StatutPaiementAbonnement.valueOf(statut);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
