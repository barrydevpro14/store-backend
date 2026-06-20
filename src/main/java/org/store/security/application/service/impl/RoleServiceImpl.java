package org.store.security.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.security.application.dto.RoleListResponse;
import org.store.security.application.dto.RoleRequest;
import org.store.security.application.dto.RoleResponse;
import org.store.security.application.dto.RoleUpdateRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.PermissionsDomainService;
import org.store.security.domain.service.RoleDomainService;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Manages roles: scoped listing by caller level, custom role CRUD per
 * company, and permission resolution. System roles (entreprise = null)
 * are immutable — mutating endpoints reject them with 403.
 */
@Service
@Transactional(readOnly = true)
public class RoleServiceImpl implements IRoleService {

    private final RoleDomainService roleDomainService;
    private final PermissionsDomainService permissionsDomainService;
    private final EntrepriseDomainService entrepriseDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public RoleServiceImpl(RoleDomainService roleDomainService,
                           PermissionsDomainService permissionsDomainService,
                           EntrepriseDomainService entrepriseDomainService,
                           ICurrentUserService currentUserService,
                           ValidatorService validatorService) {
        this.roleDomainService = roleDomainService;
        this.permissionsDomainService = permissionsDomainService;
        this.entrepriseDomainService = entrepriseDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    @Override
    public Role findByLibelle(String libelle) {
        return roleDomainService.findByLibelle(libelle)
                .orElseThrow(() -> new EntityException("role.notFound", libelle));
    }

    @Override
    public Role findById(UUID id) {
        return roleDomainService.findById(id);
    }

    @Override
    public List<RoleListResponse> findAllSystem() {
        return roleDomainService.findAllSystem();
    }

    @Override
    public List<RoleListResponse> findAllEmployee() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return roleDomainService.findAssignableByEntreprise(entrepriseId);
    }

    @Override
    public List<RoleListResponse> findAllForManagement() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return roleDomainService.findAllByEntreprise(entrepriseId);
    }

    @Override
    public RoleResponse findByIdWithPermissions(UUID id) {
        return roleDomainService.findByIdWithPermissions(id)
                .orElseThrow(() -> new EntityException("role.notFound", id.toString()));
    }

    @Override
    @Transactional
    public RoleResponse createSystemRole(RoleRequest request) {
        validatorService.validate(request);

        if (roleDomainService.findByLibelle(request.libelle()).isPresent()) {
            throw new BadArgumentException("role.alreadyExists", request.libelle());
        }

        Role role = roleDomainService.create(request.libelle(), request.description(), true);

        if (request.permissions() != null && !request.permissions().isEmpty()) {
            roleDomainService.setPermissions(role, resolvePermissions(request.permissions()));
        }

        return new RoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse createCustomRole(RoleRequest request) {
        validatorService.validate(request);

        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();

        roleDomainService.ensureLibelleValidForCreate(request.libelle(), entrepriseId);

        Entreprise entreprise = entrepriseDomainService.findById(entrepriseId);
        Role role = roleDomainService.createCustom(request.libelle(), request.description(), entreprise);

        if (request.permissions() != null && !request.permissions().isEmpty()) {
            roleDomainService.setPermissions(role, resolvePermissions(request.permissions()));
        }

        return new RoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse updateSystemRole(UUID id, RoleUpdateRequest request) {
        validatorService.validate(request);

        Role role = roleDomainService.findById(id);
        if (!role.isSysteme()) {
            throw new BadArgumentException("role.notSystemRole", role.getLibelle());
        }

        roleDomainService.findByLibelle(request.libelle())
                .filter(found -> !found.getId().equals(id))
                .ifPresent(found -> { throw new BadArgumentException("role.alreadyExists", request.libelle()); });

        return new RoleResponse(roleDomainService.updateLibelleDescription(role, request.libelle(), request.description()));
    }

    @Override
    @Transactional
    public RoleResponse update(UUID id, RoleUpdateRequest request) {
        validatorService.validate(request);
        Role role = ensureCustomAndOwned(id);

        UUID entrepriseId = role.getEntreprise().getId();
        roleDomainService.ensureLibelleValidForUpdate(request.libelle(), entrepriseId, id);

        return new RoleResponse(roleDomainService.updateLibelleDescription(role, request.libelle(), request.description()));
    }

    @Override
    @Transactional
    public RoleResponse updatePermissions(UUID id, List<String> permissionCodes) {
        Role role = ensureCustomAndOwned(id);
        roleDomainService.setPermissions(role, resolvePermissions(permissionCodes));
        return new RoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse updateSystemRolePermissions(UUID id, List<String> permissionCodes) {
        UserPrincipal caller = currentUserService.getCurrent();
        if (!caller.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            throw new BadArgumentException("role.systemRoleImmutable", id.toString());
        }

        Role role = roleDomainService.findById(id);
        if (!role.isSysteme()) {
            throw new BadArgumentException("role.notSystemRole", role.getLibelle());
        }

        roleDomainService.setPermissions(role, resolvePermissions(permissionCodes));
        return new RoleResponse(role);
    }

    @Override
    @Transactional
    public RoleResponse activate(UUID id) {
        Role role = ensureCustomAndOwned(id);
        return new RoleResponse(roleDomainService.activate(role));
    }

    @Override
    @Transactional
    public RoleResponse deactivate(UUID id) {
        Role role = ensureCustomAndOwned(id);
        return new RoleResponse(roleDomainService.deactivate(role));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Role role = ensureCustomAndOwned(id);
        if (roleDomainService.existsUserWithRole(role)) {
            throw new BadArgumentException("role.hasAssignedUsers", role.getLibelle());
        }
        roleDomainService.delete(role);
    }

    private Set<Permissions> resolvePermissions(List<String> codes) {
        Set<Permissions> result = new LinkedHashSet<>();
        codes.forEach(code -> permissionsDomainService.findByCode(code).ifPresent(result::add));
        return result;
    }

    private Role ensureCustomAndOwned(UUID id) {
        Role role = roleDomainService.findById(id);
        if (role.isSysteme()) {
            throw new BadArgumentException("role.systemRoleImmutable", role.getLibelle());
        }
        UserPrincipal caller = currentUserService.getCurrent();
        UUID callerEntreprise = caller.entrepriseId();
        if (callerEntreprise == null || !role.getEntreprise().getId().equals(callerEntreprise)) {
            throw new EntityException("role.notFound", id.toString());
        }
        return role;
    }
}
