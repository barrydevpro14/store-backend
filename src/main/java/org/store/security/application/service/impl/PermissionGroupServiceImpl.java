package org.store.security.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.security.application.dto.PermissionGroupResponse;
import org.store.security.application.service.IPermissionGroupService;
import org.store.security.domain.service.PermissionGroupDomainService;

import java.util.List;

/**
 * Lecture seule des groupes de permissions RBAC.
 * Utilisé par les dialogs d'administration des rôles système.
 */
@Service
@Transactional(readOnly = true)
public class PermissionGroupServiceImpl implements IPermissionGroupService {

    private final PermissionGroupDomainService permissionGroupDomainService;

    public PermissionGroupServiceImpl(PermissionGroupDomainService permissionGroupDomainService) {
        this.permissionGroupDomainService = permissionGroupDomainService;
    }

    @Override
    public List<PermissionGroupResponse> findAll() {
        return permissionGroupDomainService.findAllWithPermissions().stream()
                .map(PermissionGroupResponse::new)
                .toList();
    }

    @Override
    public List<PermissionGroupResponse> findAllForCustomRole() {
        return permissionGroupDomainService.findGroupsWithAssignablePermissions().stream()
                .map(PermissionGroupResponse::new)
                .toList();
    }
}
