package org.store.activite.application.dto;

import org.store.activite.domain.model.ActiviteEconomique;

import java.util.UUID;

public record ActiviteEconomiqueSummaryResponse(
        UUID id,
        String libelle
) {
    public ActiviteEconomiqueSummaryResponse(ActiviteEconomique activite) {
        this(activite.getId(), activite.getLibelle());
    }
}
