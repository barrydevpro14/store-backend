package org.store.users.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;


import java.util.UUID;

public record EmployeRequest(
        @NotBlank @Size(min = 3, max = 50) String username,
        @Valid @NotNull UtilisateurRequest utilisateur,
        @NotNull UUID roleId,
        @NotNull UUID magasinId
) {
}
