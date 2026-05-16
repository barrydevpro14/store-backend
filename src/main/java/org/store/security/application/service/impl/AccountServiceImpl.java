package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.tools.NameHelper;
import org.store.common.tools.UuidHelper;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

import java.util.Objects;
import java.util.Optional;
import java.util.UUID;

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
        String hashedPassword = passwordEncoder.encode(accountRequest.password());
        return accountDomainService.create(accountRequest.username(), hashedPassword, role);
    }

    @Override
    public Account findByUsername(String username) {
        return accountDomainService.findByUsername(username)
                .orElseThrow(() -> new EntityException("account.notFound", username));
    }

    /** Lecture safe d'un Account par id (retourne empty si introuvable, sans throw). */
    @Override
    public Optional<Account> findOptionalById(UUID accountId) {
        return accountDomainService.findOptionalById(accountId);
    }

    /** Résout un createdBy (accountId stringifié) en UserSummaryResponse(id, nomComplet) ou empty si introuvable. */
    @Override
    public Optional<UserSummaryResponse> findUserSummaryByAccountId(String accountIdString) {
        return UuidHelper.parseOptional(accountIdString)
                .flatMap(accountDomainService::findOptionalById)
                .map(Account::getUser)
                .filter(Objects::nonNull)
                .map(user -> new UserSummaryResponse(user.getId(), NameHelper.formatNomComplet(user.getNom(), user.getPrenom())));
    }

    @Override
    public Account setEnabled(Account account, boolean enabled) {
        return accountDomainService.setEnabled(account, enabled);
    }
}
