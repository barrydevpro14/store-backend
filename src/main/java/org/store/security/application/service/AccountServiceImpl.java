package org.store.security.application.service;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

@Service
public class AccountServiceImpl implements IAccountService {

    private final AccountRepository accountRepository;
    private final PasswordEncoder passwordEncoder;

    public AccountServiceImpl(AccountRepository accountRepository, PasswordEncoder passwordEncoder) {
        this.accountRepository = accountRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public Account create(AccountRequest accountRequest, Role role) {
        if (accountRepository.findByUsername(accountRequest.username()).isPresent()) {
            throw new UniqueResourceException("account.username.exists", accountRequest.username());
        }
        Account account = new Account();
        account.setUsername(accountRequest.username());
        account.setPassword(passwordEncoder.encode(accountRequest.password()));
        account.setEnabled(true);
        account.setLocked(false);
        account.setRole(role);
        return accountRepository.save(account);
    }
}
