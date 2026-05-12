package org.store.security.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

@Service
public class AccountServiceImpl implements IAccountService {

    private final AccountDomainService accountDomainService;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountDomainService accountDomainService, PasswordEncoder passwordEncoder) {
        this.accountDomainService = accountDomainService;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account create(AccountRequest accountRequest, Role role) {
        if (accountDomainService.findByUsername(accountRequest.username()).isPresent()) {
            throw new UniqueResourceException("account.username.exists", accountRequest.username());
        }
        Account account = new Account();
        account.setUsername(accountRequest.username());
        account.setPassword(passwordEncoder.encode(accountRequest.password()));
        account.setEnabled(true);
        account.setLocked(false);
        account.setRole(role);
        return accountDomainService.save(account);
    }

    @Override
    public Account findByUsername(String username) {
        return accountDomainService.findByUsername(username)
                .orElseThrow(() -> new EntityException("account.notFound", username));
    }
}
