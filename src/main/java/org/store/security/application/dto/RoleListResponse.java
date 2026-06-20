package org.store.security.application.dto;

import org.store.security.domain.model.Role;

import java.util.UUID;

/**
 * Représentation allégée d'un {@link Role} pour les endpoints de liste.
 * Ne contient pas les permissions — utiliser {@link RoleResponse} via
 * {@code GET /api/v1/roles/{id}} pour obtenir le détail complet.
 */
public record RoleListResponse(
        UUID id,
        String libelle,
        String description,
        boolean assignableToEmploye,
        boolean systeme,
        boolean actif
) {
    public RoleListResponse(Role role) {
        this(
                role.getId(),
                role.getLibelle(),
                role.getDescription(),
                role.isAssignableToEmploye(),
                role.isSysteme(),
                role.isActif()
        );
    }
}
