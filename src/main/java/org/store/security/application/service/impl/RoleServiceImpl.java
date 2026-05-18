package org.store.security.application.service.impl;

import org.store.security.application.service.IRoleService;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.RoleDomainService;

@Service
public class RoleServiceImpl implements IRoleService {

    private final RoleDomainService roleDomainService;

    public RoleServiceImpl(RoleDomainService roleDomainService) {
        this.roleDomainService = roleDomainService;
    }

    @Override
    public Role findByLibelle(String libelle) {
        return roleDomainService.findByLibelle(libelle)
                .orElseThrow(() -> new EntityException("role.notFound", libelle));
    }
}
