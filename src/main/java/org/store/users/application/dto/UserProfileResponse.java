package org.store.users.application.dto;

import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Utilisateur;
import org.store.users.presentation.UserProfileController;

import java.util.UUID;

public record UserProfileResponse(
        UUID userId,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        String username,
        String role,
        String type,
        UUID magasinId,
        String photo
) {
    public UserProfileResponse(Utilisateur utilisateur) {
        this(
                utilisateur.getId(),
                utilisateur.getNom(),
                utilisateur.getPrenom(),
                utilisateur.getEmail(),
                utilisateur.getTelephone(),
                utilisateur.getAdresse(),
                utilisateur.getAccount().getUsername(),
                utilisateur.getAccount().getRole().getLibelle(),
                utilisateur.getClass().getSimpleName().toUpperCase(),
                utilisateur instanceof Employe employe ? employe.getMagasin().getId() : null,
                utilisateur.getPhoto() != null ? UserProfileController.BASE_PATH + "/photo" : null
        );
    }
}
