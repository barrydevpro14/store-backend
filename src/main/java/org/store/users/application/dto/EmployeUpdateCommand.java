package org.store.users.application.dto;

public record EmployeUpdateCommand(
        String nom,
        String prenom,
        String email,
        String telephone,
        String adresse
) {
}
