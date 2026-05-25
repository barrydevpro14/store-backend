package org.store.config;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.store.security.application.dto.UserPrincipal;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class AuditorAwareImplTest {

    private final AuditorAwareImpl auditorAware = new AuditorAwareImpl();

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void getCurrentAuditor_should_return_empty_when_no_authentication() {
        SecurityContextHolder.clearContext();

        Optional<String> result = auditorAware.getCurrentAuditor();

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentAuditor_should_return_empty_when_authentication_is_null() {
        SecurityContextHolder.getContext().setAuthentication(null);

        Optional<String> result = auditorAware.getCurrentAuditor();

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentAuditor_should_return_empty_when_not_authenticated() {
        Authentication unauthenticated = new AnonymousAuthenticationToken(
                "key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")));
        SecurityContextHolder.getContext().setAuthentication(unauthenticated);

        Optional<String> result = auditorAware.getCurrentAuditor();

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentAuditor_should_return_empty_when_principal_is_not_UserPrincipal() {
        Authentication auth = new UsernamePasswordAuthenticationToken(
                "plain-string-principal", "credentials");
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> result = auditorAware.getCurrentAuditor();

        assertThat(result).isEmpty();
    }

    @Test
    void getCurrentAuditor_should_return_account_id_string_when_UserPrincipal_authenticated() {
        UUID accountId = UUID.randomUUID();
        UserPrincipal principal = new UserPrincipal(
                accountId, UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "admin", "ADMIN", List.of());

        Authentication auth = new UsernamePasswordAuthenticationToken(
                principal, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
        SecurityContextHolder.getContext().setAuthentication(auth);

        Optional<String> result = auditorAware.getCurrentAuditor();

        assertThat(result).contains(accountId.toString());
    }
}
