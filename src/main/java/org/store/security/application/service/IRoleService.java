package org.store.security.application.service;

import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.dto.RoleUpdateRequest;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.Set;
import java.util.UUID;

public interface IRoleService {

    Role findByLibelle(String libelle);

    Role findById(UUID id);

    /** Returns roles visible to the current authenticated user (scoped by company + role level). */
    List<RoleResponse> findAllScoped();

    RoleResponse create(RoleRequest request);

    RoleResponse update(UUID id, RoleUpdateRequest request);

    RoleResponse updatePermissions(UUID id, List<String> permissionCodes);

    RoleResponse activate(UUID id);

    RoleResponse deactivate(UUID id);

    void delete(UUID id);

    /** Résout une liste de codes de permissions en entités Permissions, en ignorant les codes inconnus. */
    Set<Permissions> resolvePermissions(List<String> codes);
}
