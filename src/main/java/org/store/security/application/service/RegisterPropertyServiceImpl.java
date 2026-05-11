package org.store.security.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.repository.PlanAbonnementRepository;
import org.store.common.exceptions.EntityException;
import org.store.magasin.application.service.IEntrepriseService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.security.domain.repository.RoleRepository;
import org.store.users.application.service.IProprietaireService;
import org.store.users.domain.model.Proprietaire;

@Service
public class RegisterPropertyServiceImpl implements IRegisterPropertyService {

    private static final String ROLE_PROPRIETAIRE = "PROPRIETAIRE";

    private final IAccountService accountService;
    private final IProprietaireService proprietaireService;
    private final IEntrepriseService entrepriseService;
    private final IMagasinService magasinService;
    private final IAbonnementService abonnementService;
    private final RoleRepository roleRepository;
    private final PlanAbonnementRepository planAbonnementRepository;
    private final IJwtService jwtService;
    private final IUserPrincipalFactory userPrincipalFactory;
    private final IRefreshTokenService refreshTokenService;

    public RegisterPropertyServiceImpl(IAccountService accountService,
                                       IProprietaireService proprietaireService,
                                       IEntrepriseService entrepriseService,
                                       IMagasinService magasinService,
                                       IAbonnementService abonnementService,
                                       RoleRepository roleRepository,
                                       PlanAbonnementRepository planAbonnementRepository,
                                       IJwtService jwtService,
                                       IUserPrincipalFactory userPrincipalFactory,
                                       IRefreshTokenService refreshTokenService) {
        this.accountService = accountService;
        this.proprietaireService = proprietaireService;
        this.entrepriseService = entrepriseService;
        this.magasinService = magasinService;
        this.abonnementService = abonnementService;
        this.roleRepository = roleRepository;
        this.planAbonnementRepository = planAbonnementRepository;
        this.jwtService = jwtService;
        this.userPrincipalFactory = userPrincipalFactory;
        this.refreshTokenService = refreshTokenService;
    }

    @Override
    @Transactional
    public AuthResponse register(RegisterPropertyRequest request) {
        Role role = roleRepository.findByLibelle(ROLE_PROPRIETAIRE)
                .orElseThrow(() -> new EntityException("role.notFound", ROLE_PROPRIETAIRE));

        PlanAbonnement plan = planAbonnementRepository.findFirstByTrialTrueAndActifTrue()
                .orElseThrow(() -> new EntityException("plan.trial.notFound"));

        Account account = accountService.create(request.account(), role);
        Proprietaire proprietaire = proprietaireService.create(request.utilisateur(), account);
        account.setUser(proprietaire);
        Entreprise entreprise = entrepriseService.create(request.entreprise(), proprietaire);
        Magasin magasin = magasinService.create(request.magasin(), entreprise);
        proprietaire.setEntreprise(entreprise);
        entreprise.setMagasins(java.util.List.of(magasin));
        abonnementService.createTrial(entreprise, plan);

        UserPrincipal principal = userPrincipalFactory.build(account);
        String accessToken = jwtService.generateToken(principal);
        String refreshToken = refreshTokenService.create(account);
        return new AuthResponse(accessToken, refreshToken);
    }
}
