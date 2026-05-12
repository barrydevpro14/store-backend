package org.store.security.application.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

@Service
public class RoleServiceImpl implements IRoleService {

    private final RoleRepository roleRepository;

    public RoleServiceImpl(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Override
    public Role findByLibelle(String libelle) {
        return roleRepository.findByLibelle(libelle)
                .orElseThrow(() -> new EntityException("role.notFound", libelle));
    }
}
