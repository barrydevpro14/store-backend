package org.store.depense.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.repository.DepenseRepository;

@Service
public class DepenseDomainService extends GlobalService<Depense, DepenseRepository> {
    public DepenseDomainService(DepenseRepository repository) {
        super(repository);
    }
}
