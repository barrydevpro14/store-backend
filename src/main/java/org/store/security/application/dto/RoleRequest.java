package org.store.security.application.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.util.List;

public record RoleRequest(
        @NotBlank @Size(max = 100) String libelle,
        @Size(max = 255) String description,
        List<String> permissions
) {
}
