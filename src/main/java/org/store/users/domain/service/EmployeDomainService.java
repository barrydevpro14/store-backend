package org.store.users.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateCommand;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Employe;
import org.store.users.domain.repository.EmployeRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class EmployeDomainService extends GlobalService<Employe, EmployeRepository> {
    public EmployeDomainService(EmployeRepository repository) {
        super(repository);
    }

    /** Retourne les comptes actifs des employés d'un magasin avec un rôle donné. */
    public List<Account> findActiveAccountsByMagasinIdAndRoleLibelle(UUID magasinId, String roleLibelle) {
        return repository.findActiveAccountsByMagasinIdAndRoleLibelle(magasinId, roleLibelle);
    }

    /** Retourne le nombre d'employés par entreprise pour le reporting ADMIN. */
    public Map<UUID, Long> countByEntrepriseId() {
        return repository.countAllGroupByEntrepriseId().stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    public boolean existsByMagasinIdAndRolePermissionCode(UUID magasinId, String permissionCode) {
        return repository.existsByMagasinIdAndRolePermissionCode(magasinId, permissionCode);
    }

    public Optional<Employe> findOptionalById(UUID id) {
        return repository.findById(id);
    }

    /** Listing projete scope entreprise (multi-tenant). */
    public Page<EmployeResponse> findResponsesByFilter(EmployeFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    /** Detail projete scope entreprise. */
    public Optional<EmployeResponse> findResponseById(UUID id, UUID entrepriseId) {
        return repository.findResponseById(id, entrepriseId);
    }

    public EmployeResponse create(UtilisateurRequest utilisateurRequest, Account account, Magasin magasin) {
        Employe employe = new Employe();
        employe.setAccount(account);
        employe.setNom(utilisateurRequest.nom());
        employe.setPrenom(utilisateurRequest.prenom());
        employe.setEmail(utilisateurRequest.email());
        employe.setTelephone(utilisateurRequest.telephone());
        employe.setAdresse(utilisateurRequest.adresse());
        employe.setMagasin(magasin);

        Employe saved = save(employe);
        account.setUser(saved);

        return new EmployeResponse(saved);
    }

    /** Met a jour les informations personnelles d'un employe (Person fields). Role et magasin geres separement. */
    public Employe update(Employe employe, EmployeUpdateCommand command) {
        employe.setNom(command.nom());
        employe.setPrenom(command.prenom());
        employe.setEmail(command.email());
        employe.setTelephone(command.telephone());
        employe.setAdresse(command.adresse());
        return save(employe);
    }

    /** Change le role de l'employe (impact permissions au prochain login JWT). */
    public Employe changeRole(Employe employe, Role role) {
        employe.getAccount().setRole(role);
        return save(employe);
    }

    /** Change le magasin de rattachement de l'employe. */
    public Employe changeMagasin(Employe employe, Magasin magasin) {
        employe.setMagasin(magasin);
        return save(employe);
    }
}
