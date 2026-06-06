package org.store.contact.application.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ContactMessageRequest(
        @NotBlank @Size(min = 2, max = 100) String nom,
        @NotBlank @Email String email,
        @NotBlank @Size(min = 3, max = 200) String sujet,
        @NotBlank @Size(min = 10) String message
) {
}
