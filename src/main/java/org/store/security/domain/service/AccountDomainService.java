package org.store.security.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.AccountRepository;

import java.util.Optional;
import java.util.UUID;

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

    public boolean existsByUsername(String username) {
        return repository.existsByUsername(username);
    }

    public Page<Account> findAllByRoleLibelle(String roleLibelle, Pageable pageable) {
        return repository.findAllByRoleLibelle(roleLibelle, pageable);
    }

    public Optional<Account> findOptionalById(UUID id) {
        return repository.findById(id);
    }

    /** Active ou desactive un Account (bloque/autorise le login). */
    public Account setEnabled(Account account, boolean enabled) {
        account.setEnabled(enabled);
        return save(account);
    }

    /** Remplace le mot de passe hash. Le caller est responsable du hashage (PasswordEncoder). */
    public Account changePassword(Account account, String hashedPassword) {
        account.setPassword(hashedPassword);
        return save(account);
    }
}
