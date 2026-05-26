package org.store.security.application.service;

import org.store.security.application.service.impl.LoginServiceImpl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.security.application.dto.AuthResponse;
import org.store.security.application.dto.LoginRequest;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class LoginServiceImplTest {

    @Mock private AuthenticationManager authenticationManager;
    @Mock private IAccountService accountService;
    @Mock private IAbonnementService abonnementService;
    @Mock private IJwtService jwtService;
    @Mock private IUserPrincipalFactory userPrincipalFactory;
    @Mock private IRefreshTokenService refreshTokenService;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;

    @InjectMocks
    private LoginServiceImpl service;

    @Test
    void should_return_tokens_when_owner_has_active_subscription() {
        LoginRequest request = new LoginRequest("john.doe", "S3cretPwd!");
        UUID entrepriseId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.doe");
        UserPrincipal principal = new UserPrincipal(account.getId(), UUID.randomUUID(), entrepriseId, null, "john.doe", null, null, "OWNER", List.of());

        when(accountService.findByUsername("john.doe")).thenReturn(account);
        when(userPrincipalFactory.build(account)).thenReturn(principal);
        when(abonnementService.hasActiveSubscription(entrepriseId)).thenReturn(true);
        when(jwtService.generateToken(principal)).thenReturn("access.token");
        when(refreshTokenService.create(account)).thenReturn("refresh-uuid");

        AuthResponse response = service.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token");
        assertThat(response.refreshToken()).isEqualTo("refresh-uuid");
        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
    }

    @Test
    void should_skip_subscription_gate_when_admin_principal_has_no_entreprise() {
        LoginRequest request = new LoginRequest("admin", "passer123");
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("admin");
        UserPrincipal principal = new UserPrincipal(account.getId(), null, null, "admin", null, null, "ADMIN", List.of());

        when(accountService.findByUsername("admin")).thenReturn(account);
        when(userPrincipalFactory.build(account)).thenReturn(principal);
        when(jwtService.generateToken(principal)).thenReturn("access.token");
        when(refreshTokenService.create(account)).thenReturn("refresh-uuid");

        AuthResponse response = service.login(request);

        assertThat(response.accessToken()).isEqualTo("access.token");
        verify(abonnementService, never()).hasActiveSubscription(any());
    }

    @Test
    void should_reject_login_when_owner_has_no_active_subscription() {
        LoginRequest request = new LoginRequest("john.doe", "S3cretPwd!");
        UUID entrepriseId = UUID.randomUUID();
        Account account = new Account();
        account.setId(UUID.randomUUID());
        account.setUsername("john.doe");
        UserPrincipal principal = new UserPrincipal(account.getId(), UUID.randomUUID(), entrepriseId, null, "john.doe", null, null, "OWNER", List.of());

        when(accountService.findByUsername("john.doe")).thenReturn(account);
        when(userPrincipalFactory.build(account)).thenReturn(principal);
        when(abonnementService.hasActiveSubscription(entrepriseId)).thenReturn(false);

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(ForbiddenException.class);

        verify(jwtService, never()).generateToken(any());
        verify(refreshTokenService, never()).create(any());
    }

    @Test
    void should_propagate_bad_credentials_when_authentication_fails() {
        LoginRequest request = new LoginRequest("john.doe", "wrong");
        when(authenticationManager.authenticate(any()))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(BadCredentialsException.class);

        verify(accountService, never()).findByUsername(any());
        verify(refreshTokenService, never()).create(any());
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void should_propagate_entity_exception_when_account_not_found_after_authentication() {
        LoginRequest request = new LoginRequest("ghost", "pwd");
        when(accountService.findByUsername("ghost"))
                .thenThrow(new EntityException("account.notFound", "ghost"));

        assertThatThrownBy(() -> service.login(request))
                .isInstanceOf(EntityException.class);

        verify(refreshTokenService, never()).create(any());
    }
}
