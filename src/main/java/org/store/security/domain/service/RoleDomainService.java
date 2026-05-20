package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.Optional;

@Service
public class RoleDomainService extends GlobalService<Role, RoleRepository> {
    public RoleDomainService(RoleRepository repository) {
        super(repository);
    }

    public Optional<Role> findByLibelle(String libelle) {
        return repository.findByLibelle(libelle);
    }

    /** Crée et persiste un nouveau Role avec libellé + description + flag `assignableToEmploye`, set de permissions vide. */
    public Role create(String libelle, String description, boolean assignableToEmploye) {
        Role role = new Role();
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setAssignableToEmploye(assignableToEmploye);
        role.setPermissions(new LinkedHashSet<>());
        return save(role);
    }
}
