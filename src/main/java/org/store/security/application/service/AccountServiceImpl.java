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
    public Account create(AccountRequest info, Role role) {
        if (accountRepository.findByUsername(info.username()).isPresent()) {
            throw new UniqueResourceException("account.username.exists", info.username());
        }
        Account account = new Account();
        account.setUsername(info.username());
        account.setPassword(passwordEncoder.encode(info.password()));
        account.setEnabled(true);
        account.setLocked(false);
        account.setRole(role);
        return accountRepository.save(account);
    }
}
