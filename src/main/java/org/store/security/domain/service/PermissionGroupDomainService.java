package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.PermissionGroup;
import org.store.security.domain.repository.PermissionGroupRepository;

import java.util.List;

@Service
public class PermissionGroupDomainService extends GlobalService<PermissionGroup, PermissionGroupRepository> {

    public PermissionGroupDomainService(PermissionGroupRepository repository) {
        super(repository);
    }

    public List<PermissionGroup> findAllWithPermissions() {
        return repository.findAllWithPermissions();
    }

    public List<PermissionGroup> findGroupsWithAssignablePermissions() {
        return repository.findGroupsWithAssignablePermissions();
    }
}
