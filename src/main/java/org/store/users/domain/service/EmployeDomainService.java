package org.store.users.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
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

    /** Retourne le nombre d'employés assignés à un magasin donné. */
    public long countByMagasinId(UUID magasinId) {
        return repository.countByMagasinId(magasinId);
    }

    /** Retourne le nombre d'employés actifs (account.enabled) d'un magasin — utilisé pour le contrôle de quota. */
    public long countActifByMagasin(UUID magasinId) {
        return repository.countActifByMagasin(magasinId);
    }

    /** Vérifie que email/telephone (si renseignés) ne sont pas déjà utilisés par un autre employé de la même entreprise. */
    public void ensureOptionalContactsInEntreprise(String email, String telephone, UUID entrepriseId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndMagasinEntrepriseId(email, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndMagasinEntrepriseId(telephone, entrepriseId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /** Variante update : exclut l'employé courant de la vérification. */
    public void ensureOptionalContactsForUpdateInEntreprise(String email, String telephone, UUID entrepriseId, UUID excludeId) {
        if (StringUtils.hasText(email) && repository.existsByEmailAndMagasinEntrepriseIdAndIdNot(email, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (StringUtils.hasText(telephone) && repository.existsByTelephoneAndMagasinEntrepriseIdAndIdNot(telephone, entrepriseId, excludeId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /** Retourne les comptes actifs des employés d'un magasin avec un rôle donné. */
    public List<Account> findActiveAccountsByMagasinIdAndRoleLibelle(UUID magasinId, String roleLibelle) {
        return repository.findActiveAccountsByMagasinIdAndRoleLibelle(magasinId, roleLibelle);
    }

    /** Compte les employés d'une entreprise (pour contrôle de quota). */
    public long countByEntrepriseId(UUID entrepriseId) {
        return repository.countByEntrepriseId(entrepriseId);
    }

    /** Retourne le nombre d'employés par entreprise pour le reporting ADMIN. */
    public Map<UUID, Long> countByEntrepriseId() {
        return repository.countAllGroupByEntrepriseId().stream()
                .collect(Collectors.toMap(
                        row -> (UUID) row[0],
                        row -> (Long) row[1]
                ));
    }

    public Optional<Employe> findOptionalById(UUID id) {
        return repository.findById(id);
    }

    /** Listing projete scope entreprise (multi-tenant). */
    public Page<EmployeResponse> findResponsesByFilter(EmployeFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(
                entrepriseId,
                filter.nom(), LikePatternHelper.toLikePattern(filter.nom()),
                filter.prenom(), LikePatternHelper.toLikePattern(filter.prenom()),
                filter.role(),
                filter.magasinId(),
                filter.actif(),
                filter.startDate(),
                filter.endDate(),
                filter.toPageable());
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
        employe.setEmail(toNullIfBlank(utilisateurRequest.email()));
        employe.setTelephone(toNullIfBlank(utilisateurRequest.telephone()));
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
        employe.setEmail(toNullIfBlank(command.email()));
        employe.setTelephone(toNullIfBlank(command.telephone()));
        employe.setAdresse(command.adresse());
        return save(employe);
    }

    private static String toNullIfBlank(String value) {
        return value == null || value.isBlank() ? null : value;
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
