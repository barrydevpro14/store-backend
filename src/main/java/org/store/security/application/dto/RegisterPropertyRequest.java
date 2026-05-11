package org.store.security.application.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import org.store.magasin.application.dto.EntrepriseRequest;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.users.application.dto.UtilisateurRequest;

public record RegisterPropertyRequest(
        @Valid @NotNull AccountRequest account,
        @Valid @NotNull UtilisateurRequest utilisateur,
        @Valid @NotNull EntrepriseRequest entreprise,
        @Valid @NotNull MagasinRequest magasin
) {
}
