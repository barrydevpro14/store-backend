package org.store.security.application.service;

import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.domain.model.Role;

import java.util.List;
import java.util.UUID;

public interface IRoleService {

    Role findByLibelle(String libelle);

    List<RoleResponse> findAll();

    RoleResponse create(RoleRequest request);

    RoleResponse updatePermissions(UUID id, List<String> permissionCodes);
}
