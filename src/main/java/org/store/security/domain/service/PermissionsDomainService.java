package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.repository.PermissionsRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class PermissionsDomainService extends GlobalService<Permissions, PermissionsRepository> {
    public PermissionsDomainService(PermissionsRepository repository) {
        super(repository);
    }

    public List<String> findAllByRoleId(UUID roleId) {
        return repository.findAllByRoleId(roleId);
    }

    public Optional<Permissions> findByCode(String code) {
        return repository.findByCode(code);
    }

    /** Crée et persiste une nouvelle Permission identifiée par son code. */
    public Permissions create(String code) {
        Permissions permission = new Permissions();
        permission.setCode(code);
        return save(permission);
    }
}
