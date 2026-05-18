package org.store.users.application.service.impl;

import org.store.users.application.service.IProprietaireService;

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
        return proprietaireDomainService.create(utilisateurRequest, account);
    }
}
