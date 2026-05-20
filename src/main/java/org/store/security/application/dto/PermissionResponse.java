package org.store.security.application.dto;

import org.store.security.domain.model.Permissions;

import java.util.UUID;

/**
 * Lecture-seule d'une {@link Permissions} (référentiel RBAC). Code utilisé
 * comme clé applicative (`EMPLOYE_CREATE`, `SALE_APPROVE`, …) — c'est cette
 * chaîne que le frontend doit matcher pour gater des UI.
 */
public record PermissionResponse(UUID id, String code) {
    public PermissionResponse(Permissions permission) {
        this(permission.getId(), permission.getCode());
    }
}
