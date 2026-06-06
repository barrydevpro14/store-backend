package org.store.users.application.service.impl;

import org.store.users.application.service.IProprietaireService;

import org.springframework.stereotype.Service;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.service.ProprietaireDomainService;
import org.store.users.domain.service.UtilisateurDomainService;

/** Creates a Proprietaire user by enforcing contact uniqueness (email + phone) then delegating to the domain layer. */
@Service
public class ProprietaireServiceImpl implements IProprietaireService {

    private final ProprietaireDomainService proprietaireDomainService;
    private final UtilisateurDomainService utilisateurDomainService;

    public ProprietaireServiceImpl(ProprietaireDomainService proprietaireDomainService,
                                   UtilisateurDomainService utilisateurDomainService) {
        this.proprietaireDomainService = proprietaireDomainService;
        this.utilisateurDomainService = utilisateurDomainService;
    }

    @Override
    public Proprietaire create(UtilisateurRequest utilisateurRequest, Account account) {
        utilisateurDomainService.ensureContactsAvailable(utilisateurRequest.email(), utilisateurRequest.telephone());
        return proprietaireDomainService.create(utilisateurRequest, account);
    }
}
