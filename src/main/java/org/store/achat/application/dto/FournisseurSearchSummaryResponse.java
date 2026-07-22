package org.store.achat.application.dto;

import org.store.achat.domain.model.Fournisseur;

import java.util.UUID;

public record FournisseurSearchSummaryResponse(
        UUID id,
        String nom,
        String telephone,
        String reference,
        boolean systeme
) {
    public FournisseurSearchSummaryResponse(Fournisseur fournisseur) {
        this(fournisseur.getId(), fournisseur.getNom(), fournisseur.getTelephone(),
                fournisseur.getReference(), fournisseur.isSysteme());
    }
}
