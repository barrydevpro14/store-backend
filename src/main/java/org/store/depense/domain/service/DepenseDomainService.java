package org.store.depense.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.repository.DepenseJpaRepository;

@Service
public class DepenseDomainService extends GlobalService<Depense, DepenseJpaRepository> {
    public DepenseDomainService(DepenseJpaRepository repository) {
        super(repository);
    }
}
