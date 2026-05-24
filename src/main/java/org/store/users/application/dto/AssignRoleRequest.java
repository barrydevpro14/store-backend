package org.store.users.application.dto;

import jakarta.validation.constraints.NotBlank;

public record AssignRoleRequest(
        @NotBlank String role
) {
}
