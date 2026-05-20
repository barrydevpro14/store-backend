package org.store.security.application.service.impl;

import org.store.security.application.dto.RoleResponse;
import org.store.security.application.service.IRoleService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.RoleDomainService;

import java.util.List;

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

    /**
     * Liste plate des rôles avec leurs codes de permissions. `Role.permissions`
     * étant `@ManyToMany` LAZY, on garde la session ouverte le temps de la
     * projection RoleResponse(role) — d'où le `@Transactional(readOnly=true)`.
     */
    @Override
    @Transactional(readOnly = true)
    public List<RoleResponse> findAll() {
        return roleDomainService.findAll().stream()
                .map(RoleResponse::new)
                .toList();
    }
}
