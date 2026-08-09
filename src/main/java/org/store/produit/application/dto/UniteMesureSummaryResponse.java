package org.store.produit.application.dto;

import org.store.produit.domain.model.UniteMesure;

import java.util.UUID;

public record UniteMesureSummaryResponse(
        UUID id,
        String code,
        String libelle,
        String symbole
) {
    public UniteMesureSummaryResponse(UniteMesure uniteMesure) {
        this(
                uniteMesure.getId(),
                uniteMesure.getCode(),
                uniteMesure.getLibelle(),
                uniteMesure.getSymbole()
        );
    }
}
