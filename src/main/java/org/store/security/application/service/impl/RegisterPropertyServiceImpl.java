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
        CreateResult result = doCreate(request);
        UserPrincipal principal = userPrincipalFactory.build(result.account());
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(result.account());
        return new AuthResponse(accessToken, refreshToken);
    }

    @Override
    @Transactional
    public EntrepriseResponse adminCreate(RegisterPropertyRequest request) {
        return new EntrepriseResponse(doCreate(request).entreprise());
    }

    private CreateResult doCreate(RegisterPropertyRequest request) {
        Role role = roleService.findByLibelle(ROLE_PROPRIETAIRE);
        PlanAbonnement plan = planAbonnementService.findFirstTrialActif();

        Account account = accountService.create(request.account(), role);
        Proprietaire proprietaire = proprietaireService.create(request.utilisateur(), account);
        account.setUser(proprietaire);
        Entreprise entreprise = entrepriseService.create(request.entreprise(), proprietaire);
        Magasin magasin = magasinService.create(request.magasin(), entreprise);
        proprietaire.setEntreprise(entreprise);
        entreprise.setMagasins(List.of(magasin));
        abonnementService.createTrial(entreprise, plan);

        return new CreateResult(account, entreprise);
    }

    private record CreateResult(Account account, Entreprise entreprise) {}
}
