package org.store.security.application.dto;

import org.store.security.domain.model.PermissionGroup;

import java.util.List;
import java.util.UUID;

/**
 * Lecture seule d'un {@link PermissionGroup} avec ses permissions.
 * Utilisé par {@code GET /api/v1/permission-groups} pour alimenter
 * les dialogs de gestion des rôles sans traitement côté frontend.
 */
public record PermissionGroupResponse(
        UUID id,
        String libelle,
        String description,
        List<PermissionResponse> permissions
) {
    public PermissionGroupResponse(PermissionGroup group) {
        this(
                group.getId(),
                group.getLibelle(),
                group.getDescription(),
                group.getPermissions().stream()
                        .map(PermissionResponse::new)
                        .toList()
        );
    }
}
