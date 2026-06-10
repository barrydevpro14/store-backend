package org.store.users.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.store.common.validation.Phone;

import java.util.UUID;

public record EmployeUpdateRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank @Email String email,
        @NotBlank @Phone String telephone,
        String adresse,
        @NotNull UUID roleId,
        @NotNull UUID magasinId
) {
}
