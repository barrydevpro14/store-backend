package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.users.domain.model.Employe;
import org.store.users.domain.repository.EmployeRepository;

@Service
public class EmployeDomainService extends GlobalService<Employe, EmployeRepository> {
    public EmployeDomainService(EmployeRepository repository) {
        super(repository);
    }
}
