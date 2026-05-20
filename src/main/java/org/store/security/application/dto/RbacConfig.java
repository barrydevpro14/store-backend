package org.store.security.application.dto;

import java.util.List;

public record RbacConfig(
        List<String> permissions,
        List<RoleDef> roles
) {
    /**
     * Définition d'un rôle dans le YAML RBAC. `assignableToEmploye` est
     * absent par défaut (interprété comme `false`) — seuls MANAGER et
     * VENDEUR doivent le passer à `true`. Le sync DB applique strictement
     * la valeur du YAML.
     */
    public record RoleDef(
            String libelle,
            String description,
            Boolean assignableToEmploye,
            List<String> permissions
    ) {
        public boolean assignableToEmployeOrFalse() {
            return assignableToEmploye != null && assignableToEmploye;
        }
    }
}
