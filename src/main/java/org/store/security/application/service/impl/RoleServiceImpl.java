package org.store.security.application.service.impl;

import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.service.IRoleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/** Manages application roles: lookup by name, full listing, and creation of custom assignable roles with their permissions. */
@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements IRoleService {

    private final RoleDomainService roleDomainService;
    private final PermissionsDomainService permissionsDomainService;
    private final ValidatorService validatorService;

    public RoleServiceImpl(RoleDomainService roleDomainService,
                           PermissionsDomainService permissionsDomainService,
                           ValidatorService validatorService) {
        this.roleDomainService = roleDomainService;
        this.permissionsDomainService = permissionsDomainService;
        this.validatorService = validatorService;
    }

    @Override
    public Role findByLibelle(String libelle) {
        return roleDomainService.findByLibelle(libelle)
                .orElseThrow(() -> new EntityException("role.notFound", libelle));
    }

    @Override
    public List<RoleResponse> findAll() {
        return roleDomainService.findAll().stream()
                .map(RoleResponse::new)
                .toList();
    }

    /** Crée un rôle personnalisé assignable aux employés. */
    @Override
    @Transactional
    public RoleResponse create(RoleRequest request) {
        validatorService.validate(request);

        if (roleDomainService.findByLibelle(request.libelle()).isPresent()) {
            throw new BadArgumentException("role.alreadyExists", request.libelle());
        }

        Role role = roleDomainService.create(request.libelle(), request.description(), true);

        if (request.permissions() != null && !request.permissions().isEmpty()) {
            Set<Permissions> perms = resolvePermissions(request.permissions());
            roleDomainService.setPermissions(role, perms);
        }

        return new RoleResponse(role);
    }

    /** Remplace la liste des permissions d'un rôle personnalisé. */
    @Override
    @Transactional
    public RoleResponse updatePermissions(UUID id, List<String> permissionCodes) {
        Role role = roleDomainService.findById(id);
        Set<Permissions> perms = resolvePermissions(permissionCodes);
        roleDomainService.setPermissions(role, perms);
        return new RoleResponse(role);
    }

    public Set<Permissions> resolvePermissions(List<String> codes) {
        Set<Permissions> result = new LinkedHashSet<>();

        codes.forEach(code -> permissionsDomainService.findByCode(code).ifPresent(result::add));

        return result;
    }
}
