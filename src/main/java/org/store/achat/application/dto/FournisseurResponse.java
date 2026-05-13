package org.store.achat.application.dto;

import org.store.achat.domain.model.Fournisseur;

import java.util.UUID;

public record FournisseurResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        String reference,
        String origine,
        UUID entrepriseId
) {
    public FournisseurResponse(Fournisseur fournisseur) {
        this(
                fournisseur.getId(),
                fournisseur.getNom(),
                fournisseur.getPrenom(),
                fournisseur.getEmail(),
                fournisseur.getTelephone(),
                fournisseur.getAdresse(),
                fournisseur.getReference(),
                fournisseur.getOrigine(),
                fournisseur.getEntreprise().getId()
        );
    }
}
