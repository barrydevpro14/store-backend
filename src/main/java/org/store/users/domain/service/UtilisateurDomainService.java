package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.repository.UtilisateurRepository;

@Service
public class UtilisateurDomainService extends GlobalService<Utilisateur, UtilisateurRepository> {
    public UtilisateurDomainService(UtilisateurRepository repository) {
        super(repository);
    }
}
