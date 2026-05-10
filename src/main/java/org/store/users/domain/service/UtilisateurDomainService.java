package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.repository.UtilisateurJpaRepository;

@Service
public class UtilisateurDomainService extends GlobalService<Utilisateur, UtilisateurJpaRepository> {
    public UtilisateurDomainService(UtilisateurJpaRepository repository) {
        super(repository);
    }
}
