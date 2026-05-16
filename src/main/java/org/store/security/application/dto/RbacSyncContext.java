package org.store.security.application.dto;

import org.store.security.domain.model.Permissions;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * État accumulé pendant un cycle de synchronisation RBAC : catalogue des permissions résolues
 * et listes mutables de tracking (créations / mises à jour). Passé en paramètre unique aux
 * méthodes internes du `RolesPermissionsSyncServiceImpl` pour respecter la règle 30.
 */
public record RbacSyncContext(
        Map<String, Permissions> catalog,
        List<String> addedPermissions,
        List<String> addedRoles,
        List<String> updatedRoles
) {
    public RbacSyncContext() {
        this(new LinkedHashMap<>(), new ArrayList<>(), new ArrayList<>(), new ArrayList<>());
    }
}
