package org.store.security.application.service;

import org.springframework.stereotype.Service;
import org.store.security.domain.service.PermissionsDomainService;

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
}
