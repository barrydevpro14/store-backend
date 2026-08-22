package org.store.users.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.store.common.validation.OwnerValidation;
import org.store.common.validation.Phone;

public record UtilisateurRequest(
        @NotBlank String nom,
        @NotBlank String prenom,
        @NotBlank(groups = OwnerValidation.class) @Email String email,
        @NotBlank(groups = OwnerValidation.class) @Phone String telephone,
        String adresse
) {
}
