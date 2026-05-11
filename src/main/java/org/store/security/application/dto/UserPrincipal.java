package org.store.security.application.dto;

import java.util.List;
import java.util.UUID;

public record UserPrincipal(
        UUID userId,
        UUID entrepriseId,
        UUID magasinId,
        String username,
        List<String> permissions
) {
    public boolean hasPermission(String permission) {
        return permissions != null && permissions.contains(permission);
    }
}