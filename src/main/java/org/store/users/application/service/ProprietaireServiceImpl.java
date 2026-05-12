package org.store.users.application.service;

import org.springframework.stereotype.Service;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.service.ProprietaireDomainService;

@Service
public class ProprietaireServiceImpl implements IProprietaireService {

    private final ProprietaireDomainService proprietaireDomainService;

    public ProprietaireServiceImpl(ProprietaireDomainService proprietaireDomainService) {
        this.proprietaireDomainService = proprietaireDomainService;
    }

    @Override
    public Proprietaire create(UtilisateurRequest utilisateurRequest, Account account) {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setAccount(account);
        proprietaire.setNom(utilisateurRequest.nom());
        proprietaire.setPrenom(utilisateurRequest.prenom());
        proprietaire.setEmail(utilisateurRequest.email());
        proprietaire.setTelephone(utilisateurRequest.telephone());
        proprietaire.setAdresse(utilisateurRequest.adresse());
        return proprietaireDomainService.save(proprietaire);
    }
}
