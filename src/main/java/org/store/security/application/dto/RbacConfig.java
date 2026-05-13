package org.store.security.application.dto;

import java.util.List;

public record RbacConfig(
        List<String> permissions,
        List<RoleDef> roles
) {
    public record RoleDef(
            String libelle,
            String description,
            List<String> permissions
    ) {
    }
}
