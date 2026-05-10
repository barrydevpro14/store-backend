package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Account;
import org.store.security.domain.repository.AccountJpaRepository;

@Service
public class AccountDomainService extends GlobalService<Account, AccountJpaRepository> {
    public AccountDomainService(AccountJpaRepository repository) {
        super(repository);
    }
}
