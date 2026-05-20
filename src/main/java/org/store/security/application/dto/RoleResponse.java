package org.store.security.application.dto;

import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.UUID;

/**
 * Lecture-seule d'un {@link Role} (référentiel RBAC). Inclut la liste
 * triée des codes de permissions et le flag explicite
 * `assignableToEmploye` (mirroir DB) que le frontend utilise pour filtrer
 * ses selectors "rôle d'un employé" sans heuristique permission-based.
 */
public record RoleResponse(
        UUID id,
        String libelle,
        String description,
        boolean assignableToEmploye,
        List<String> permissions
) {
    public RoleResponse(Role role) {
        this(
                role.getId(),
                role.getLibelle(),
                role.getDescription(),
                role.isAssignableToEmploye(),
                role.getPermissions().stream()
                        .map(Permissions::getCode)
                        .sorted()
                        .toList()
        );
    }
}
