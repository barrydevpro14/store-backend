package org.store.notification.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.domain.model.Echeance;
import org.store.notification.domain.repository.EcheanceJpaRepository;

@Service
public class EcheanceDomainService extends GlobalService<Echeance, EcheanceJpaRepository> {
    public EcheanceDomainService(EcheanceJpaRepository repository) {
        super(repository);
    }
}
