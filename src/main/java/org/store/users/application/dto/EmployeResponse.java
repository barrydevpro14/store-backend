package org.store.users.application.dto;

import org.store.magasin.application.dto.MagasinSummaryResponse;
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
        RoleSummary role,
        MagasinSummaryResponse magasin,
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
                new RoleSummary(employe.getAccount().getRole().getId(), employe.getAccount().getRole().getLibelle()),
                new MagasinSummaryResponse(employe.getMagasin()),
                employe.getAccount().isEnabled()
        );
    }
}
