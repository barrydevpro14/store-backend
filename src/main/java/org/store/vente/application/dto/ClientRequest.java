package org.store.vente.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.store.common.validation.Phone;

import java.util.UUID;

public record ClientRequest(
        @NotBlank @Size(max = 255) String nom,
        @Size(max = 255) String prenom,
        @Email @Size(max = 255) String email,
        @Phone @Size(max = 30) String telephone,
        @Size(max = 255) String adresse,
        @NotNull UUID magasinId
) {
}
