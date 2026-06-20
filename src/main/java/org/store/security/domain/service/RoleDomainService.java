package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.RoleListResponse;
import org.store.security.application.dto.RoleResponse;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@Service
public class RoleDomainService extends GlobalService<Role, RoleRepository> {

    public RoleDomainService(RoleRepository repository) {
        super(repository);
    }

    public Optional<Role> findByLibelle(String libelle) {
        return repository.findByLibelle(libelle);
    }

    public List<RoleListResponse> findAllSystem() {
        return repository.findAllSystem();
    }

    public Optional<RoleResponse> findByIdWithPermissions(UUID id) {
        return repository.findByIdEager(id).map(RoleResponse::new);
    }

    public List<RoleListResponse> findAssignableByEntreprise(UUID entrepriseId) {
        return repository.findAssignableByEntreprise(entrepriseId);
    }

    public List<RoleListResponse> findAllByEntreprise(UUID entrepriseId) {
        return repository.findAllByEntreprise(entrepriseId);
    }

    public boolean existsUserWithRole(Role role) {
        return repository.existsUserWithRole(role);
    }

    public boolean existsByLibelleAndEntreprise(String libelle, UUID entrepriseId) {
        return repository.existsByLibelleAndEntreprise(libelle, entrepriseId);
    }

    public boolean existsByLibelleSystem(String libelle) {
        return repository.existsByLibelleSystem(libelle);
    }

    public boolean existsByLibelleAndEntrepriseExcluding(String libelle, UUID entrepriseId, UUID excludeId) {
        return repository.existsByLibelleAndEntrepriseExcluding(libelle, entrepriseId, excludeId);
    }

    public void ensureLibelleValidForCreate(String libelle, UUID entrepriseId) {
        if (existsByLibelleSystem(libelle)) {
            throw new BadArgumentException("role.conflictsWithSystemRole", libelle);
        }
        if (existsByLibelleAndEntreprise(libelle, entrepriseId)) {
            throw new BadArgumentException("role.alreadyExists", libelle);
        }
    }

    public void ensureLibelleValidForUpdate(String libelle, UUID entrepriseId, UUID excludeId) {
        if (existsByLibelleSystem(libelle)) {
            throw new BadArgumentException("role.conflictsWithSystemRole", libelle);
        }
        if (existsByLibelleAndEntrepriseExcluding(libelle, entrepriseId, excludeId)) {
            throw new BadArgumentException("role.alreadyExists", libelle);
        }
    }

    /** Crée un rôle personnalisé scoped à une entreprise. */
    public Role createCustom(String libelle, String description, Entreprise entreprise) {
        Role role = new Role();
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setAssignableToEmploye(true);
        role.setSysteme(false);
        role.setEntreprise(entreprise);
        role.setActif(true);
        role.setPermissions(new LinkedHashSet<>());
        return save(role);
    }

    /** Crée et persiste un rôle système (entreprise = null). */
    public Role create(String libelle, String description, boolean assignableToEmploye) {
        Role role = new Role();
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setAssignableToEmploye(assignableToEmploye);
        role.setSysteme(true);
        role.setActif(true);
        role.setPermissions(new LinkedHashSet<>());
        return save(role);
    }

    public Role setPermissions(Role role, Set<Permissions> permissions) {
        role.setPermissions(permissions);
        return save(role);
    }

    public Role updateLibelleDescription(Role role, String libelle, String description) {
        role.setLibelle(libelle);
        role.setDescription(description);
        return save(role);
    }

    public Role activate(Role role) {
        role.setActif(true);
        return save(role);
    }

    public Role deactivate(Role role) {
        role.setActif(false);
        return save(role);
    }
}
