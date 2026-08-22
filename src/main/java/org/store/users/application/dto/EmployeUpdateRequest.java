package org.store.users.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record EmployeUpdateRequest(
        @Valid @NotNull UtilisateurRequest utilisateur,
        @NotNull UUID roleId,
        @NotNull UUID magasinId
) {
}
