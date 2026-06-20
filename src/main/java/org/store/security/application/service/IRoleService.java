package org.store.security.application.service;

import org.store.security.application.dto.RoleListResponse;
import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.dto.RoleUpdateRequest;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface IRoleService {

    Role findByLibelle(String libelle);

    Role findById(UUID id);

    /** Returns system roles (entreprise IS NULL) without permissions — lightweight list. */
    List<RoleListResponse> findAllSystem();

    /** Returns assignable active roles for the caller's company — lightweight list, no permissions. */
    List<RoleListResponse> findAllEmployee();

    /** Returns all company roles (any status) + system assignable roles — for OWNER role management. */
    List<RoleListResponse> findAllForManagement();

    /** Returns a single role with its permissions eagerly loaded — for detail and permission-edit views. */
    RoleResponse findByIdWithPermissions(UUID id);

    /** Creates a new system role (entreprise = null). Caller must be ADMIN. */
    RoleResponse createSystemRole(RoleRequest request);

    /** Creates a new custom role scoped to the caller's company. Caller must be OWNER. */
    RoleResponse createCustomRole(RoleRequest request);

    RoleResponse update(UUID id, RoleUpdateRequest request);

    /** Updates libelle and description of a system role. Caller must be ADMIN. */
    RoleResponse updateSystemRole(UUID id, RoleUpdateRequest request);

    RoleResponse updatePermissions(UUID id, List<String> permissionCodes);

    /** Updates permissions on a system role (entreprise = null). Caller must be ADMIN. */
    RoleResponse updateSystemRolePermissions(UUID id, List<String> permissionCodes);

    RoleResponse activate(UUID id);

    RoleResponse deactivate(UUID id);

    void delete(UUID id);
}
