package org.store.notification.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.notification.domain.model.Echeance;
import org.store.notification.domain.repository.EcheanceRepository;

@Service
public class EcheanceDomainService extends GlobalService<Echeance, EcheanceRepository> {
    public EcheanceDomainService(EcheanceRepository repository) {
        super(repository);
    }
}
