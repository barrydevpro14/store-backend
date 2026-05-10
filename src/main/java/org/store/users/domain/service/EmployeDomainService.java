package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.users.domain.model.Employe;
import org.store.users.domain.repository.EmployeJpaRepository;

@Service
public class EmployeDomainService extends GlobalService<Employe, EmployeJpaRepository> {
    public EmployeDomainService(EmployeJpaRepository repository) {
        super(repository);
    }
}
