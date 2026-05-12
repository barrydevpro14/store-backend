package org.store.users.application.service;

import org.store.users.application.service.impl.EmployeServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.ForbiddenException;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.AccountRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.application.service.IPermissionsService;
import org.store.security.application.service.IRoleService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.EmployeRequest;
import org.store.users.application.dto.EmployeResponse;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.service.EmployeDomainService;

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
class EmployeServiceImplTest {

    @Mock private EmployeDomainService employeDomainService;
    @Mock private IAccountService accountService;
    @Mock private IRoleService roleService;
    @Mock private IPermissionsService permissionsService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;

    @InjectMocks
    private EmployeServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Magasin magasin;
    private UtilisateurRequest validUtilisateur;
    private AccountRequest validAccount;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        validUtilisateur = new UtilisateurRequest("Doe", "John", "john@example.com", "770000000", "Dakar");
        validAccount = new AccountRequest("john.emp", "S3cretPwd!");
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, magasinId, "owner", "PROPRIETAIRE",
                List.of("PROPRIETAIRE_ACCESS", "EMPLOYE_CREATE"));
    }

    private UserPrincipal manager() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, magasinId, "manager", "MANAGER",
                List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));
    }

    private EmployeRequest request(String role, UUID requestedMagasinId) {
        return new EmployeRequest(validAccount, validUtilisateur, role, requestedMagasinId);
    }

    private EmployeResponse sampleResponse(String role) {
        return new EmployeResponse(UUID.randomUUID(), "Doe", "John",
                "john@example.com", "770000000", "Dakar", "john.emp", role, magasinId);
    }

    private Role roleWithId() {
        Role r = new Role();
        r.setId(UUID.randomUUID());
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
        when(roleService.findByLibelle("MANAGER")).thenReturn(managerRole);
        when(permissionsService.findAllByRoleId(managerRole.getId()))
                .thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(employeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, "EMPLOYE_CREATE")).thenReturn(false);
        when(accountService.create(eq(validAccount), eq(managerRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request("MANAGER", magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void proprietaire_should_create_vendeur_in_owned_magasin() {
        Role vendeurRole = roleWithId();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        EmployeResponse expected = sampleResponse("VENDEUR");

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findByLibelle("VENDEUR")).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(accountService.create(eq(validAccount), eq(vendeurRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request("VENDEUR", magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void manager_should_create_vendeur_in_his_magasin() {
        Role vendeurRole = roleWithId();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        EmployeResponse expected = sampleResponse("VENDEUR");

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findByLibelle("VENDEUR")).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(accountService.create(eq(validAccount), eq(vendeurRole))).thenReturn(account);
        when(employeDomainService.create(eq(validUtilisateur), eq(account), eq(magasin))).thenReturn(expected);

        EmployeResponse response = service.create(request("VENDEUR", magasinId));

        assertThat(response).isSameAs(expected);
    }

    @Test
    void should_be_forbidden_when_role_does_not_grant_employe_access() {
        Role propRole = roleWithId();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findByLibelle("PROPRIETAIRE")).thenReturn(propRole);
        when(permissionsService.findAllByRoleId(propRole.getId())).thenReturn(List.of("PROPRIETAIRE_ACCESS"));

        assertThatThrownBy(() -> service.create(request("PROPRIETAIRE", magasinId)))
                .isInstanceOf(ForbiddenException.class);

        verify(magasinService, never()).findById(any());
        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void manager_should_be_forbidden_to_create_role_with_employe_create() {
        Role managerRole = roleWithId();
        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findByLibelle("MANAGER")).thenReturn(managerRole);
        when(permissionsService.findAllByRoleId(managerRole.getId()))
                .thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));

        assertThatThrownBy(() -> service.create(request("MANAGER", magasinId)))
                .isInstanceOf(ForbiddenException.class);

        verify(magasinService, never()).findById(any());
        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void proprietaire_should_be_forbidden_when_magasin_already_has_a_manager() {
        Role managerRole = roleWithId();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(roleService.findByLibelle("MANAGER")).thenReturn(managerRole);
        when(permissionsService.findAllByRoleId(managerRole.getId()))
                .thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(employeDomainService.existsByMagasinIdAndRolePermissionCode(magasinId, "EMPLOYE_CREATE")).thenReturn(true);

        assertThatThrownBy(() -> service.create(request("MANAGER", magasinId)))
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
        when(roleService.findByLibelle("VENDEUR")).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(foreignMagasinId)).thenReturn(foreignMagasin);
        when(magasinService.ensureAccessibleByCurrentUser(foreignMagasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(request("VENDEUR", foreignMagasinId)))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }

    @Test
    void manager_should_be_forbidden_when_targeting_another_magasin() {
        Role vendeurRole = roleWithId();
        UUID otherMagasinId = UUID.randomUUID();
        Magasin otherMagasin = new Magasin();
        otherMagasin.setId(otherMagasinId);

        when(currentUserService.getCurrent()).thenReturn(manager());
        when(roleService.findByLibelle("VENDEUR")).thenReturn(vendeurRole);
        when(permissionsService.findAllByRoleId(vendeurRole.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));
        when(magasinService.findById(otherMagasinId)).thenReturn(otherMagasin);
        when(magasinService.ensureAccessibleByCurrentUser(otherMagasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(request("VENDEUR", otherMagasinId)))
                .isInstanceOf(ForbiddenException.class);

        verify(accountService, never()).create(any(), any());
        verify(employeDomainService, never()).create(any(), any(), any());
    }
}
