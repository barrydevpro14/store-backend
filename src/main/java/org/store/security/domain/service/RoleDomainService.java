package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

@Service
public class RoleDomainService extends GlobalService<Role, RoleRepository> {
    public RoleDomainService(RoleRepository repository) {
        super(repository);
    }
}
