package org.store.abonnement.application.dto;

import jakarta.validation.constraints.Min;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.common.validation.EnumValue;

import java.util.UUID;

public record AbonnementFilter(
        UUID entrepriseId,
        @EnumValue(enumClass = AbonnementStatut.class) String statut,
        UUID planId,
        @Min(0) int page,
        @Min(1) int size
) {
    public AbonnementStatut statutAsEnum() {
        return statut == null || statut.isBlank() ? null : AbonnementStatut.valueOf(statut);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
    }
}
