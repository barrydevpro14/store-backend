package org.store.users.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record AssignRoleRequest(
        @NotNull UUID roleId
) {
}
