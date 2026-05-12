package org.store.users.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.IPermissionsService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.domain.service.EmployeDomainService;

import java.util.List;

@Service
public class EmployeServiceImpl implements IEmployeService {

    private final EmployeDomainService employeDomainService;
    private final IAccountService accountService;
    private final IRoleService roleService;
    private final IPermissionsService permissionsService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;

    public EmployeServiceImpl(EmployeDomainService employeDomainService,
                              IAccountService accountService,
                              IRoleService roleService,
                              IPermissionsService permissionsService,
                              IMagasinService magasinService,
                              ICurrentUserService currentUserService) {
        this.employeDomainService = employeDomainService;
        this.accountService = accountService;
        this.roleService = roleService;
        this.permissionsService = permissionsService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
    }

    @Override
    @Transactional
    public EmployeResponse create(EmployeRequest employeRequest) {
        UserPrincipal currentUser = currentUserService.getCurrent();

        Role role = roleService.findByLibelle(employeRequest.role());
        List<String> rolePermissions = permissionsService.findAllByRoleId(role.getId());
        boolean askedRoleIsElevated = rolePermissions.contains(PermissionCode.EMPLOYE_CREATE.name());

        if (!rolePermissions.contains(PermissionCode.EMPLOYE_ACCESS.name())) {
            throw new ForbiddenException("employe.create.role.notAllowed", employeRequest.role());
        }
        if (askedRoleIsElevated && !currentUser.hasPermission(PermissionCode.PROPRIETAIRE_ACCESS)) {
            throw new ForbiddenException("employe.create.elevatedRole.forbidden");
        }

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(
                magasinService.findById(employeRequest.magasinId())
        );

        if (askedRoleIsElevated
                && employeDomainService.existsByMagasinIdAndRolePermissionCode(magasin.getId(), PermissionCode.EMPLOYE_CREATE.name())) {
            throw new ForbiddenException("magasin.alreadyHasManager");
        }

        Account account = accountService.create(employeRequest.account(), role);

        return employeDomainService.create(employeRequest.utilisateur(), account, magasin);
    }
}
