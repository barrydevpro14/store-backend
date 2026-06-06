package org.store.achat.application.dto;

import org.store.achat.domain.model.Fournisseur;

import java.util.UUID;

public record FournisseurSummaryResponse(
        UUID id,
        String nom
) {
    public FournisseurSummaryResponse(Fournisseur fournisseur) {
        this(fournisseur.getId(), fournisseur.getNom());
    }
}
