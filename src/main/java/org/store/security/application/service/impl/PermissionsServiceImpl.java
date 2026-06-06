package org.store.security.application.service.impl;

import org.store.security.application.dto.PermissionResponse;
import org.store.security.application.service.IPermissionsService;

import org.springframework.stereotype.Service;
import org.store.security.domain.service.PermissionsDomainService;

import java.util.Comparator;
import java.util.List;
import java.util.UUID;

@Service
public class PermissionsServiceImpl implements IPermissionsService {

    private final PermissionsDomainService permissionsDomainService;

    public PermissionsServiceImpl(PermissionsDomainService permissionsDomainService) {
        this.permissionsDomainService = permissionsDomainService;
    }

    @Override
    public List<String> findAllByRoleId(UUID roleId) {
        return permissionsDomainService.findAllByRoleId(roleId);
    }

    /**
     * Liste plate des permissions du référentiel, triée par code pour
     * faciliter l'affichage côté UI (référentiel RBAC ~ 80 entrées,
     * coût du tri Java négligeable).
     */
    @Override
    public List<PermissionResponse> findAll() {
        return permissionsDomainService.findAll().stream()
                .map(PermissionResponse::new)
                .sorted(Comparator.comparing(PermissionResponse::code))
                .toList();
    }
}
