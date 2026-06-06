package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.entreprise.domain.model.Entreprise;
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

    public List<Role> findAllWithPermissions() {
        return repository.findAllWithPermissions();
    }

    public List<Role> findByEntrepriseIdOrGlobal(UUID entrepriseId) {
        return repository.findByEntrepriseIdOrGlobal(entrepriseId);
    }

    public boolean existsUserWithRole(Role role) {
        return repository.existsUserWithRole(role);
    }

    public boolean existsByLibelleAndEntreprise(String libelle, UUID entrepriseId) {
        return repository.existsByLibelleAndEntreprise(libelle, entrepriseId);
    }

    public boolean existsByLibelleAndEntrepriseExcluding(String libelle, UUID entrepriseId, UUID excludeId) {
        return repository.existsByLibelleAndEntrepriseExcluding(libelle, entrepriseId, excludeId);
    }

    /** Crée un rôle personnalisé scoped à une entreprise. */
    public Role createCustom(String libelle, String description, Entreprise entreprise) {
        Role role = new Role();
        role.setLibelle(libelle);
        role.setDescription(description);
        role.setAssignableToEmploye(true);
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
