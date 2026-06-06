package org.store.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Identifiant de connexion (username ou e-mail) pour déclencher le reset. */
public record ForgotPasswordRequest(
        @NotBlank @Size(max = 100) String identifier
) {}
