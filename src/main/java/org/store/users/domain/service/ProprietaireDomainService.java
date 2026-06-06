package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class ProprietaireDomainService extends GlobalService<Proprietaire, ProprietaireRepository> {
    public ProprietaireDomainService(ProprietaireRepository repository) {
        super(repository);
    }

    /** Retourne le compte du propriétaire d'une entreprise. */
    public Optional<Account> findAccountByEntrepriseId(UUID entrepriseId) {
        return repository.findAccountByEntrepriseId(entrepriseId);
    }

    public Proprietaire create(UtilisateurRequest utilisateurRequest, Account account) {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setAccount(account);
        proprietaire.setNom(utilisateurRequest.nom());
        proprietaire.setPrenom(utilisateurRequest.prenom());
        proprietaire.setEmail(utilisateurRequest.email());
        proprietaire.setTelephone(utilisateurRequest.telephone());
        proprietaire.setAdresse(utilisateurRequest.adresse());
        return save(proprietaire);
    }
}
