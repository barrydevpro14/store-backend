package org.store.users.application.dto;

import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

public record ProprietaireSummaryResponse(
        UUID id,
        String username,
        String nom,
        String prenom,
        String email,
        String telephone
) {
    public ProprietaireSummaryResponse(Proprietaire proprietaire) {
        this(
                proprietaire.getId(),
                proprietaire.getAccount() != null ? proprietaire.getAccount().getUsername() : null,
                proprietaire.getNom(),
                proprietaire.getPrenom(),
                proprietaire.getEmail(),
                proprietaire.getTelephone()
        );
    }
}
