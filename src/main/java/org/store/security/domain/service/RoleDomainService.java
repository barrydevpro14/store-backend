package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleJpaRepository;

@Service
public class RoleDomainService extends GlobalService<Role, RoleJpaRepository> {
    public RoleDomainService(RoleJpaRepository repository) {
        super(repository);
    }
}
