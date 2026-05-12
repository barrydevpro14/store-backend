package org.store.security.application.service.impl;

import org.store.security.application.service.*;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.domain.model.PlanAbonnement;
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

@Service
public class RegisterPropertyServiceImpl implements IRegisterPropertyService {

    private static final String ROLE_PROPRIETAIRE = "PROPRIETAIRE";

    private final IAccountService accountService;
    private final IProprietaireService proprietaireService;
    private final IEntrepriseService entrepriseService;
    private final IMagasinService magasinService;
    private final IAbonnementService abonnementService;
    private final IRoleService roleService;
    private final IPlanAbonnementService planAbonnementService;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final IRefreshTokenService refreshTokenService;

    public RegisterPropertyServiceImpl(IAccountService accountService,
                                       IProprietaireService proprietaireService,
                                       IEntrepriseService entrepriseService,
                                       IMagasinService magasinService,
                                       IAbonnementService abonnementService,
                                       IRoleService roleService,
                                       IPlanAbonnementService planAbonnementService,
                                       IJwtService jwtService,
                                       IUserPrincipalFactory userPrincipalFactory,
                                       IRefreshTokenService refreshTokenService) {
        this.accountService = accountService;
        this.proprietaireService = proprietaireService;
        this.entrepriseService = entrepriseService;
        this.magasinService = magasinService;
        this.abonnementService = abonnementService;
        this.roleService = roleService;
        this.planAbonnementService = planAbonnementService;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterPropertyRequest request) {
        Account account = createAccount(request, ROLE_PROPRIETAIRE);
        UserPrincipal principal = userPrincipalFactory.build(account);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(account);
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public AccountResponse registerOwnerByAdmin(RegisterPropertyRequest request) {
        return new AccountResponse(createAccount(request, ROLE_PROPRIETAIRE));
    }

    @Override
    @Transactional
    public EntrepriseResponse registerEntrepriseByAdmin(RegisterPropertyRequest request) {
        Account account = createAccount(request, ROLE_PROPRIETAIRE);
        Proprietaire proprietaire = (Proprietaire) account.getUser();
        return new EntrepriseResponse(proprietaire.getEntreprise());
    }

    @Override
    @Transactional
    public Account createAccount(RegisterPropertyRequest request, String roleName) {
        Role role = roleService.findByLibelle(roleName);
        PlanAbonnement plan = planAbonnementService.findFirstTrialActif();

        Account account = accountService.create(request.account(), role);
        Proprietaire proprietaire = proprietaireService.create(request.utilisateur(), account);
        account.setUser(proprietaire);
        Entreprise entreprise = entrepriseService.create(request.entreprise(), proprietaire);
        Magasin magasin = magasinService.create(request.magasin(), entreprise);
        proprietaire.setEntreprise(entreprise);
        entreprise.setMagasins(List.of(magasin));
        abonnementService.createTrial(entreprise, plan);

        return account;
    }
}
