package org.store.security.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.Optional;

@Service
public class AccountDomainService extends GlobalService<Account, AccountRepository> {
    public AccountDomainService(AccountRepository repository) {
        super(repository);
    }

    public Account create(String username, String hashedPassword, Role role) {
        Account account = new Account();
        account.setUsername(username);
        account.setPassword(hashedPassword);
        account.setEnabled(true);
        account.setLocked(false);
        account.setRole(role);
        return save(account);
    }

    public Optional<Account> findByUsername(String username) {
        return repository.findByUsername(username);
    }
}
