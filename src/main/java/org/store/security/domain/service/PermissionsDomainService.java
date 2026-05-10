package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.repository.PermissionsJpaRepository;

@Service
public class PermissionsDomainService extends GlobalService<Permissions, PermissionsJpaRepository> {
    public PermissionsDomainService(PermissionsJpaRepository repository) {
        super(repository);
    }
}
