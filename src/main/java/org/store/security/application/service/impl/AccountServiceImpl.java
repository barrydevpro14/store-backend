package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.store.common.dto.UserSummaryResponse;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.security.application.dto.AccountRequest;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;
import org.store.users.domain.model.Utilisateur;

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
        return Optional.ofNullable(accountIdString)
                .filter(s -> !s.isBlank())
                .flatMap(this::parseUuid)
                .flatMap(accountDomainService::findOptionalById)
                .map(Account::getUser)
                .filter(Objects::nonNull)
                .map(user -> new UserSummaryResponse(user.getId(), buildNomComplet(user)));
    }

    /** Parse une chaîne en UUID, retourne empty si le format est invalide. */
    private Optional<UUID> parseUuid(String value) {
        try {
            return Optional.of(UUID.fromString(value));
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    /** Concatène nom + prenom en gérant les blancs/null via `Objects.requireNonNullElse`. */
    private String buildNomComplet(Utilisateur user) {
        String nom = Objects.requireNonNullElse(user.getNom(), "").trim();
        String prenom = Objects.requireNonNullElse(user.getPrenom(), "").trim();

        if (prenom.isEmpty()) return nom;
        if (nom.isEmpty()) return prenom;
        return nom + " " + prenom;
    }
}
