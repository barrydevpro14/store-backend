package org.store.security.application.dto;

import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.UUID;

/**
 * Lecture-seule d'un {@link Role}. Inclut la liste triée des codes de
 * permissions, le flag {@code assignableToEmploye}, {@code systeme} et {@code actif}.
 */
public record RoleResponse(
        UUID id,
        String libelle,
        String description,
        boolean assignableToEmploye,
        boolean systeme,
        boolean actif,
        List<String> permissions
) {
    public RoleResponse(Role role) {
        this(
                role.getId(),
                role.getLibelle(),
                role.getDescription(),
                role.isAssignableToEmploye(),
                role.isSysteme(),
                role.isActif(),
                role.getPermissions().stream()
                        .map(Permissions::getCode)
                        .sorted()
                        .toList()
        );
    }
}
