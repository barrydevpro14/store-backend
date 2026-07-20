package org.store.activite.application.dto;

import org.store.activite.domain.model.ActiviteEconomique;

import java.util.UUID;

public record ActiviteEconomiqueResponse(
        UUID id,
        String libelle,
        String description,
        boolean actif
) {
    public ActiviteEconomiqueResponse(ActiviteEconomique activite) {
        this(activite.getId(), activite.getLibelle(), activite.getDescription(), activite.isActif());
    }
}
