package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

import java.util.Optional;

@Service
public class RoleDomainService extends GlobalService<Role, RoleRepository> {
    public RoleDomainService(RoleRepository repository) {
        super(repository);
    }

    public Optional<Role> findByLibelle(String libelle) {
        return repository.findByLibelle(libelle);
    }
}
