package org.store.users.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.ChangePasswordRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.IAccountService;
import org.store.security.application.service.ICurrentUserService;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.application.dto.UserProfileResponse;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.application.service.impl.UserProfileServiceImpl;
import org.store.users.domain.model.Employe;
import org.store.users.domain.service.UtilisateurDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserProfileServiceImplTest {

    @Mock private UtilisateurDomainService utilisateurDomainService;
    @Mock private IAccountService accountService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private UserProfileServiceImpl service;

    private UUID userId;
    private UUID magasinId;
    private Employe employe;
    private Account account;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());
        Magasin magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        Role role = new Role();
        role.setLibelle("VENDEUR");
        account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.emp");
        account.setRole(role);

        employe = new Employe();
        employe.setId(userId);
        employe.setNom("Doe");
        employe.setPrenom("John");
        employe.setEmail("john@example.com");
        employe.setTelephone("770000000");
        employe.setAdresse("Dakar");
        employe.setMagasin(magasin);
        employe.setAccount(account);
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(account.getId(), userId, UUID.randomUUID(), magasinId,
                "john.emp", "VENDEUR", List.of("AUTH_CHANGE_PASSWORD"));
    }

    @Test
    void getCurrentProfile_should_return_profile_of_authenticated_user() {
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(utilisateurDomainService.findById(userId)).thenReturn(employe);

        UserProfileResponse response = service.getCurrentProfile();

        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.username()).isEqualTo("john.emp");
        assertThat(response.role()).isEqualTo("VENDEUR");
        assertThat(response.type()).isEqualTo("EMPLOYE");
        assertThat(response.magasinId()).isEqualTo(magasinId);
    }

    @Test
    void updateCurrentProfile_should_apply_changes_via_domain_service() {
        UserProfileUpdateRequest request = new UserProfileUpdateRequest(
                "NewNom", "NewPrenom", "new@example.com", "770000001", "Saint-Louis");

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(utilisateurDomainService.findById(userId)).thenReturn(employe);
        when(utilisateurDomainService.update(eq(employe), eq(request))).thenReturn(employe);

        service.updateCurrentProfile(request);

        verify(utilisateurDomainService).update(eq(employe), eq(request));
    }

    @Test
    void changePassword_should_delegate_with_old_and_new_password() {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPwd123", "newPwd1234");

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(utilisateurDomainService.findById(userId)).thenReturn(employe);

        service.changePassword(request);

        verify(accountService).changePassword(account, "oldPwd123", "newPwd1234");
    }

    @Test
    void getCurrentProfile_should_validate_nothing_when_no_request() {
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(utilisateurDomainService.findById(userId)).thenReturn(employe);

        service.getCurrentProfile();

        verify(validatorService, org.mockito.Mockito.never()).validate(any());
    }
}
