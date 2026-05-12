package org.store.users.application.dto;

import org.store.users.domain.model.Utilisateur;

public record UserResponse(
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse
) {
    public UserResponse(Utilisateur utilisateur) {
        this(
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getAdresse()
        );
    }
}
