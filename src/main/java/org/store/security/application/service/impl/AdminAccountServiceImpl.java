package org.store.security.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.security.application.dto.AdminAccountRequest;
import org.store.security.application.dto.AdminAccountResponse;
import org.store.security.application.service.IAdminAccountService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.service.AccountDomainService;

import java.util.UUID;

/**
 * Gestion des comptes ADMIN SaaS : liste, création, activation/désactivation.
 * Accès restreint à {@code ADMIN_ACCESS}.
 */
@Service
@Transactional(readOnly = true)
public class AdminAccountServiceImpl implements IAdminAccountService {

    private final AccountDomainService accountDomainService;
    private final IRoleService roleService;
    private final PasswordEncoder passwordEncoder;
    private final ValidatorService validatorService;

    public AdminAccountServiceImpl(AccountDomainService accountDomainService,
                                   IRoleService roleService,
                                   PasswordEncoder passwordEncoder,
                                   ValidatorService validatorService) {
        this.accountDomainService = accountDomainService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.validatorService = validatorService;
    }

    @Override
    public Page<AdminAccountResponse> findAll(Pageable pageable) {
        return accountDomainService.findAllByRoleLibelle("ADMIN", pageable)
                .map(AdminAccountResponse::new);
    }

    @Override
    @Transactional
    public AdminAccountResponse create(AdminAccountRequest request) {
        validatorService.validate(request);

        if (accountDomainService.existsByUsername(request.username())) {
            throw new BadArgumentException("account.username.alreadyExists", request.username());
        }

        Role adminRole = roleService.findByLibelle("ADMIN");
        String hashedPassword = passwordEncoder.encode(request.password());
        Account account = accountDomainService.create(request.username(), hashedPassword, adminRole);
        return new AdminAccountResponse(account);
    }

    @Override
    @Transactional
    public AdminAccountResponse setEnabled(UUID id, boolean enabled) {
        Account account = accountDomainService.findById(id);
        return new AdminAccountResponse(accountDomainService.setEnabled(account, enabled));
    }
}
