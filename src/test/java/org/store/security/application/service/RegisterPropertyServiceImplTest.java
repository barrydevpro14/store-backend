package org.store.security.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPlanAbonnementService;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.common.exceptions.EntityException;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.RegisterPropertyRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.application.service.IProprietaireService;
import org.store.users.domain.model.Proprietaire;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RegisterPropertyServiceImplTest {

    @Mock private IAccountService accountService;
    @Mock private IProprietaireService proprietaireService;
    @Mock private IEntrepriseService entrepriseService;
    @Mock private IMagasinService magasinService;
    @Mock private IAbonnementService abonnementService;
    @Mock private IRoleService roleService;
    @Mock private IPlanAbonnementService planAbonnementService;
    @Mock private IJwtService jwtService;
    @Mock private IUserPrincipalFactory userPrincipalFactory;
    @Mock private IRefreshTokenService refreshTokenService;

    @InjectMocks
    private RegisterPropertyServiceImpl service;

    private RegisterPropertyRequest validRequest;

    @BeforeEach
    void setUp() {
        validRequest = new RegisterPropertyRequest(
                new AccountRequest("john.doe", "S3cretPwd!"),
                new UtilisateurRequest("Doe", "John", "john@example.com", "+221700000000", "Dakar"),
                new EntrepriseRequest("ACME", "ACME SARL", "NINEA-123", "RCCM-456", "Dakar"),
                new MagasinRequest("Magasin Centre", "Dakar Centre")
        );
    }

    @Test
    void should_return_auth_response_with_both_tokens_when_register_succeeds() {
        Role role = new Role();
        PlanAbonnement plan = new PlanAbonnement();
        Account account = accountWithId();
        Proprietaire proprietaire = new Proprietaire();
        Entreprise entreprise = entrepriseWithId();
        Magasin magasin = magasinWithId();
        UserPrincipal principal = new UserPrincipal(account.getId(), entreprise.getId(), magasin.getId(), "john.doe", "PROPRIETAIRE", List.of());

        when(roleService.findByLibelle("PROPRIETAIRE")).thenReturn(role);
        when(planAbonnementService.findFirstTrialActif()).thenReturn(plan);
        when(accountService.create(eq(validRequest.account()), eq(role))).thenReturn(account);
        when(proprietaireService.create(eq(validRequest.utilisateur()), eq(account))).thenReturn(proprietaire);
        when(entrepriseService.create(eq(validRequest.entreprise()), eq(proprietaire))).thenReturn(entreprise);
        when(magasinService.create(eq(validRequest.magasin()), eq(entreprise))).thenReturn(magasin);
        when(userPrincipalFactory.build(account)).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn("access.token");
        when(refreshTokenService.create(account)).thenReturn("refresh-uuid");

        AuthResponse response = service.register(validRequest);

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        verify(abonnementService).createTrial(entreprise, plan);
    }

    @Test
    void should_propagate_entity_exception_when_role_proprietaire_not_found() {
        when(roleService.findByLibelle("PROPRIETAIRE"))
                .thenThrow(new EntityException("role.notFound", "PROPRIETAIRE"));

        assertThatThrownBy(() -> service.register(validRequest))
                .isInstanceOf(EntityException.class);

        verify(accountService, never()).create(any(), any());
    }

    @Test
    void should_propagate_entity_exception_when_trial_plan_not_found() {
        when(roleService.findByLibelle("PROPRIETAIRE")).thenReturn(new Role());
        when(planAbonnementService.findFirstTrialActif())
                .thenThrow(new EntityException("plan.trial.notFound"));

        assertThatThrownBy(() -> service.register(validRequest))
                .isInstanceOf(EntityException.class);

        verify(accountService, never()).create(any(), any());
    }

    private Account accountWithId() {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUsername("john.doe");
        return a;
    }

    private Entreprise entrepriseWithId() {
        Entreprise e = new Entreprise();
        e.setId(UUID.randomUUID());
        return e;
    }

    private Magasin magasinWithId() {
        Magasin m = new Magasin();
        m.setId(UUID.randomUUID());
        return m;
    }
}
