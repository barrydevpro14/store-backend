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
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.service.UtilisateurDomainService;

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
    private final UtilisateurDomainService utilisateurDomainService;

    public AdminAccountServiceImpl(AccountDomainService accountDomainService,
                                   IRoleService roleService,
                                   PasswordEncoder passwordEncoder,
                                   ValidatorService validatorService,
                                   UtilisateurDomainService utilisateurDomainService) {
        this.accountDomainService = accountDomainService;
        this.roleService = roleService;
        this.passwordEncoder = passwordEncoder;
        this.validatorService = validatorService;
        this.utilisateurDomainService = utilisateurDomainService;
    }

    @Override
    public Page<AdminAccountResponse> findAll(Pageable pageable) {
        return accountDomainService.findAllByRoleLibelle("ADMIN", pageable)
                .map(AdminAccountResponse::new);
    }

    /** Crée un compte ADMIN avec son profil (nom, prénom, email, téléphone) après vérification d'unicité. */
    @Override
    @Transactional
    public AdminAccountResponse create(AdminAccountRequest adminAccountRequest) {
        validatorService.validate(adminAccountRequest);

        if (accountDomainService.existsByUsername(adminAccountRequest.username())) {
            throw new BadArgumentException("account.username.alreadyExists", adminAccountRequest.username());
        }

        utilisateurDomainService.ensureContactsAvailable(adminAccountRequest.email(), adminAccountRequest.telephone());

        Role adminRole = roleService.findByLibelle("ADMIN");
        String hashedPassword = passwordEncoder.encode(adminAccountRequest.password());
        Account account = accountDomainService.create(adminAccountRequest.username(), hashedPassword, adminRole);

        Utilisateur utilisateur = new Utilisateur();
        utilisateur.setAccount(account);
        utilisateur.setNom(adminAccountRequest.nom());
        utilisateur.setPrenom(adminAccountRequest.prenom());
        utilisateur.setEmail(adminAccountRequest.email());
        utilisateur.setTelephone(adminAccountRequest.telephone());
        utilisateurDomainService.save(utilisateur);

        return new AdminAccountResponse(account, utilisateur);
    }

    @Override
    @Transactional
    public AdminAccountResponse setEnabled(UUID id, boolean enabled) {
        Account account = accountDomainService.findById(id);
        if (account.isSysteme() && !enabled) {
            throw new BadArgumentException("account.systeme.cannotDeactivate");
        }
        return new AdminAccountResponse(accountDomainService.setEnabled(account, enabled));
    }
}
