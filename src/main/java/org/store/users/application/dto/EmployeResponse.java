package org.store.users.application.dto;

import org.store.users.domain.model.Employe;

import java.util.UUID;

public record EmployeResponse(
        UUID id,
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse,
        String username,
        UUID roleId,
        String role,
        UUID magasinId,
        boolean actif
) {
    public EmployeResponse(Employe employe) {
        this(
                employe.getId(),
                employe.getNom(),
                employe.getPrenom(),
                employe.getEmail(),
                employe.getTelephone(),
                employe.getAdresse(),
                employe.getAccount().getUsername(),
                employe.getAccount().getRole().getId(),
                employe.getAccount().getRole().getLibelle(),
                employe.getMagasin().getId(),
                employe.getAccount().isEnabled()
        );
    }
}
