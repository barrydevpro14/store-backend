package org.store.security.application.dto;

import org.store.security.application.enums.PermissionCode;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID accountId,
        UUID userId,
        UUID entrepriseId,
        UUID magasinId,
        String username,
        String currency,
        String countryName,
        String role,
        List<String> permissions
) {
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }

    public boolean hasPermission(PermissionCode permission) {
        return hasPermission(permission.name());
    }
}
