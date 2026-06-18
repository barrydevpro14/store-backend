package org.store.users.application.service;

import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.users.application.service.impl.EmployeServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.ResetPasswordRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.IPermissionsService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.EmployeFilter;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.EmployeUpdateRequest;
import org.store.users.application.dto.RoleSummary;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Employe;
import org.store.users.domain.service.EmployeDomainService;
import org.store.users.domain.service.UtilisateurDomainService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class EmployeServiceImplTest {

    @Mock private EmployeDomainService employeDomainService;
    @Mock private UtilisateurDomainService utilisateurDomainService;
    @Mock private IAccountService accountService;
    @Mock private IRoleService roleService;
    @Mock private IPermissionsService permissionsService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private org.store.notification.application.service.IEmailEventPublisher emailEventPublisher;
    @Mock private org.store.abonnement.application.service.AbonnementQuotaService quotaService;

    @InjectMocks
    private EmployeServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Magasin magasin;
    private UtilisateurRequest validUtilisateur;
    private static final String VALID_USERNAME = "john.emp";
    private static final UUID ROLE_ID = UUID.fromString("00000000-0000-0000-0000-000000000010");

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        validUtilisateur = new UtilisateurRequest("Doe", "John", "john@example.com", "+221770000000", "Dakar");
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId, "owner", null, null, "OWNER",
                List.of("OWNER_ACCESS", "EMPLOYE_CREATE"));
    }

    private UserPrincipal manager() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId, "manager", null, null, "MANAGER",
                List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));
    }

    private EmployeRequest request(UUID roleId, UUID requestedMagasinId) {
        return new EmployeRequest(VALID_USERNAME, validUtilisateur, roleId, requestedMagasinId);
    }

    private EmployeResponse sampleResponse(String roleLibelle) {
        return new EmployeResponse(UUID.randomUUID(), "Doe", "John",
                "john@example.com", "+221770000000", "Dakar", "john.emp",
                new RoleSummary(ROLE_ID, roleLibelle), new MagasinSummaryResponse(magasinId , "store-one"), true);
    }

    private Role roleWithId() {
        Role r = new Role();
        r.setId(UUID.randomUUID());
        // Tous les rôles dans ces tests sont des rôles employé (MANAGER /
        // SELLER) — sinon `ensureRoleAllowed` les rejetterait.
        r.setAssignableToEmploye(true);
        return r;
    }

    @Test
    void proprietaire_should_create_manager_in_owned_magasin() {
        Role managerRole = roleWithId();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        EmployeResponse expected = sampleResponse("MANAGER");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findById(ROLE_ID)).thenReturn(managerRole);
        when(permissionsService.findAllByRoleId(managerRole.getId()))
                .thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(accountService.create(any(AccountRequest.class), eq(managerRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request(ROLE_ID, magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void proprietaire_should_create_vendeur_in_owned_magasin() {
        Role vendeurRole = roleWithId();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        EmployeResponse expected = sampleResponse("SELLER");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findById(ROLE_ID)).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(accountService.create(any(AccountRequest.class), eq(vendeurRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request(ROLE_ID, magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void manager_should_create_vendeur_in_his_magasin() {
        Role vendeurRole = roleWithId();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        EmployeResponse expected = sampleResponse("SELLER");

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findById(ROLE_ID)).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(accountService.create(any(AccountRequest.class), eq(vendeurRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request(ROLE_ID, magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void should_be_forbidden_when_role_is_not_assignable_to_employe() {
        Role propRole = roleWithId();
        // Marque explicite : OWNER / ADMIN ne sont jamais
        // `assignableToEmploye` en base — le helper par défaut force `true`
        // pour la grande majorité des tests employé, on l'inverse ici.
        propRole.setAssignableToEmploye(false);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findById(ROLE_ID)).thenReturn(propRole);

        EmployeRequest req = request(ROLE_ID, magasinId);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);

        verify(magasinService, never()).findById(any());
        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void manager_should_be_forbidden_to_create_another_manager() {
        Role managerRole = roleWithId();
        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findById(ROLE_ID)).thenReturn(managerRole);
        when(permissionsService.findAllByRoleId(managerRole.getId()))
                .thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));

        assertThatThrownBy(() -> service.create(request(ROLE_ID, magasinId)))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void proprietaire_should_be_forbidden_when_magasin_not_in_his_entreprise() {
        Role vendeurRole = roleWithId();
        Entreprise otherEntreprise = new Entreprise();
        otherEntreprise.setId(UUID.randomUUID());
        UUID foreignMagasinId = UUID.randomUUID();
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(foreignMagasinId);
        foreignMagasin.setEntreprise(otherEntreprise);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findById(ROLE_ID)).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(foreignMagasinId)).thenReturn(foreignMagasin);
        when(magasinService.ensureAccessibleByCurrentUser(foreignMagasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        EmployeRequest req = request(ROLE_ID, foreignMagasinId);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void findAll_should_force_magasinId_to_manager_own_store() {
        EmployeFilter requested = new EmployeFilter(null, null, null, UUID.randomUUID(), null, null, null, 0, 10);

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(employeDomainService.findResponsesByFilter(any(EmployeFilter.class), eq(entrepriseId)))
                .thenAnswer(invocation -> {
                    EmployeFilter scoped = invocation.getArgument(0);
                    assertThat(scoped.magasinId()).isEqualTo(magasinId);
                    return new PageImpl<>(List.of(sampleResponse("SELLER")), PageRequest.of(0, 10), 1);
                });

        assertThat(service.findAllByCurrentEntreprise(requested).getContent()).hasSize(1);
    }

    @Test
    void findAll_should_keep_filter_as_is_for_proprietaire() {
        UUID requestedMagasin = UUID.randomUUID();
        EmployeFilter requested = new EmployeFilter(null, null, null, requestedMagasin, null, null, null, 0, 10);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findResponsesByFilter(eq(requested), eq(entrepriseId)))
                .thenReturn(new PageImpl<>(List.of(sampleResponse("SELLER")), PageRequest.of(0, 10), 1));

        assertThat(service.findAllByCurrentEntreprise(requested).getContent()).hasSize(1);
    }

    @Test
    void findResponseById_should_throw_when_not_found() {
        UUID employeId = UUID.randomUUID();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.findResponseById(employeId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void findResponseById_should_throw_for_manager_targeting_other_magasin() {
        UUID employeId = UUID.randomUUID();
        Magasin otherMagasin = new Magasin();
        otherMagasin.setId(UUID.randomUUID());
        otherMagasin.setEntreprise(magasin.getEntreprise());
        Employe other = new Employe();
        other.setId(employeId);
        other.setMagasin(otherMagasin);
        Account otherAccount = new Account();
        otherAccount.setEnabled(true);
        other.setAccount(otherAccount);

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(other));

        assertThatThrownBy(() -> service.findResponseById(employeId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void deactivate_should_call_account_setEnabled_false() {
        UUID employeId = UUID.randomUUID();
        Employe employe = new Employe();
        employe.setId(employeId);
        employe.setMagasin(magasin);
        Account account = new Account();
        account.setEnabled(true);
        employe.setAccount(account);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(employe));

        service.deactivate(employeId);

        verify(accountService).setEnabled(account, false);
    }

    @Test
    void activate_should_call_account_setEnabled_true() {
        UUID employeId = UUID.randomUUID();
        Employe employe = new Employe();
        employe.setId(employeId);
        employe.setMagasin(magasin);
        Account account = new Account();
        account.setEnabled(false);
        employe.setAccount(account);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(employe));

        service.activate(employeId);

        verify(accountService).setEnabled(account, true);
    }

    @Test
    void resetPassword_should_delegate_to_account_service() {
        UUID employeId = UUID.randomUUID();
        Employe employe = new Employe();
        employe.setId(employeId);
        employe.setMagasin(magasin);
        Account account = new Account();
        employe.setAccount(account);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(employe));

        service.resetPassword(employeId, new ResetPasswordRequest("brandnewP@ss"));

        verify(accountService).resetPassword(account, "brandnewP@ss");
    }

    @Test
    void resetPassword_should_throw_when_manager_targets_other_magasin() {
        UUID employeId = UUID.randomUUID();
        Magasin otherMagasin = new Magasin();
        otherMagasin.setId(UUID.randomUUID());
        otherMagasin.setEntreprise(magasin.getEntreprise());
        Employe other = new Employe();
        other.setId(employeId);
        other.setMagasin(otherMagasin);
        Account account = new Account();
        other.setAccount(account);

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(other));

        ResetPasswordRequest resetReq = new ResetPasswordRequest("brandnewP@ss");

        assertThatThrownBy(() -> service.resetPassword(employeId, resetReq))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).resetPassword(any(), any());
    }

    @Test
    void update_should_apply_changes_via_domain_service() {
        UUID employeId = UUID.randomUUID();
        Employe employe = new Employe();
        employe.setId(employeId);
        employe.setMagasin(magasin);
        Account account = new Account();
        account.setEnabled(true);
        Role currentRole = roleWithId();
        account.setRole(currentRole);
        employe.setAccount(account);
        Role newRole = roleWithId();
        EmployeUpdateRequest body = new EmployeUpdateRequest("Doe", "Jane", "jane@example.com",
                "+221770000001", "Dakar", ROLE_ID, magasinId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(employeDomainService.findOptionalById(employeId)).thenReturn(Optional.of(employe));
        when(roleService.findById(ROLE_ID)).thenReturn(newRole);
        when(permissionsService.findAllByRoleId(newRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);

        service.update(employeId, body);

        verify(employeDomainService).update(eq(employe), any());
        verify(employeDomainService).changeRole(employe, newRole);
        verify(employeDomainService).changeMagasin(employe, magasin);
    }

    @Test
    void manager_should_be_forbidden_when_targeting_another_magasin() {
        Role vendeurRole = roleWithId();
        UUID otherMagasinId = UUID.randomUUID();
        Magasin otherMagasin = new Magasin();
        otherMagasin.setId(otherMagasinId);

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findById(ROLE_ID)).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(otherMagasinId)).thenReturn(otherMagasin);
        when(magasinService.ensureAccessibleByCurrentUser(otherMagasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        EmployeRequest req = request(ROLE_ID, otherMagasinId);

        assertThatThrownBy(() -> service.create(req))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }
}
