package org.store.users.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.store.security.application.dto.AccountRequest;

import java.util.UUID;

public record EmployeRequest(
        @Valid @NotNull AccountRequest account,
        @Valid @NotNull UtilisateurRequest utilisateur,
        @NotBlank String role,
        @NotNull UUID magasinId
) {
}
