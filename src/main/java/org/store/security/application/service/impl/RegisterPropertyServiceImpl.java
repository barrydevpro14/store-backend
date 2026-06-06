package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.AccountResponse;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.service.IProprietaireService;
import org.store.users.domain.model.Proprietaire;

import java.util.List;

/**
 * Orchestrates the full property-owner signup flow (public + admin variants): wires the
 * Account, Proprietaire, Entreprise and initial Magasin together and creates the entreprise's
 * TRIAL Abonnement.
 */
@Service
public class RegisterPropertyServiceImpl implements IRegisterPropertyService {

    private static final String ROLE_OWNER = "OWNER";

    private final IAccountService accountService;
    private final IProprietaireService proprietaireService;
    private final IEntrepriseService entrepriseService;
    private final IMagasinService magasinService;
    private final IRoleService roleService;
    private final IAbonnementService abonnementService;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final IRefreshTokenService refreshTokenService;

    public RegisterPropertyServiceImpl(IAccountService accountService,
                                       IProprietaireService proprietaireService,
                                       IEntrepriseService entrepriseService,
                                       IMagasinService magasinService,
                                       IRoleService roleService,
                                       IAbonnementService abonnementService,
                                       IJwtService jwtService,
                                       IUserPrincipalFactory userPrincipalFactory,
                                       IRefreshTokenService refreshTokenService) {
        this.accountService = accountService;
        this.proprietaireService = proprietaireService;
        this.entrepriseService = entrepriseService;
        this.magasinService = magasinService;
        this.roleService = roleService;
        this.abonnementService = abonnementService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
    }

    /** Self-service signup for a new property owner: creates the full chain and returns access + refresh tokens. */
    @Override
    @Transactional
    public AuthResponse register(RegisterPropertyRequest registerPropertyRequest) {
        Account account = createAccount(registerPropertyRequest, ROLE_OWNER);
        UserPrincipal principal = userPrincipalFactory.build(account);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(account);
        return new AuthResponse(accessToken, refreshToken);
    }

    /** Admin-side variant: same creation flow as register but returns the account response without issuing tokens. */
    @Override
    @Transactional
    public AccountResponse registerOwnerByAdmin(RegisterPropertyRequest registerPropertyRequest) {
        return new AccountResponse(createAccount(registerPropertyRequest, ROLE_OWNER));
    }

    /** Admin-side variant focused on the entreprise payload: runs the creation flow and projects the resulting entreprise. */
    @Override
    @Transactional
    public EntrepriseResponse registerEntrepriseByAdmin(RegisterPropertyRequest registerPropertyRequest) {
        Account account = createAccount(registerPropertyRequest, ROLE_OWNER);
        Proprietaire proprietaire = (Proprietaire) account.getUser();
        return new EntrepriseResponse(proprietaire.getEntreprise());
    }

    /**
     * Creates the Account + Proprietaire + Entreprise + initial Magasin chain, then a TRIAL Abonnement
     * bound to the active trial plan. The TRIAL row carries dateDebut today and dateFin today + trial-days,
     * and surfaces as the entreprise's first {@code abonnement}.
     */
    @Override
    @Transactional
    public Account createAccount(RegisterPropertyRequest registerPropertyRequest, String roleName) {
        Role role = roleService.findByLibelle(roleName);

        Account account = accountService.create(registerPropertyRequest.account(), role);
        Proprietaire proprietaire = proprietaireService.create(registerPropertyRequest.utilisateur(), account);
        account.setUser(proprietaire);

        Entreprise entreprise = entrepriseService.create(registerPropertyRequest.entreprise(), proprietaire);
        Magasin magasin = magasinService.create(registerPropertyRequest.magasin(), entreprise);
        proprietaire.setEntreprise(entreprise);
        entreprise.setMagasins(List.of(magasin));

        abonnementService.createTrialForSignup(entreprise);

        return account;
    }
}
