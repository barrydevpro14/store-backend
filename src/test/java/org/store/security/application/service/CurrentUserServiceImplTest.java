package org.store.security.application.service;

import org.store.security.application.service.impl.CurrentUserServiceImpl;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.store.common.exceptions.UnauthorisedException;
import org.store.security.application.dto.UserPrincipal;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CurrentUserServiceImplTest {

    private final CurrentUserServiceImpl service = new CurrentUserServiceImpl();

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void should_return_user_principal_when_authenticated() {
        UserPrincipal principal = new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(),
                "owner", "PROPRIETAIRE", List.of("PROPRIETAIRE_ACCESS"));
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(principal, null, List.of(new SimpleGrantedAuthority("PROPRIETAIRE_ACCESS")))
        );

        UserPrincipal result = service.getCurrent();

        assertThat(result).isSameAs(principal);
    }

    @Test
    void should_throw_when_no_authentication() {
        SecurityContextHolder.clearContext();

        assertThatThrownBy(service::getCurrent)
                .isInstanceOf(UnauthorisedException.class);
    }

    @Test
    void should_throw_when_principal_is_not_user_principal() {
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken("not-a-user-principal", null, List.of())
        );

        assertThatThrownBy(service::getCurrent)
                .isInstanceOf(UnauthorisedException.class);
    }

    @Test
    void should_throw_when_authentication_anonymous() {
        SecurityContextHolder.getContext().setAuthentication(
                new AnonymousAuthenticationToken("key", "anonymous", List.of(new SimpleGrantedAuthority("ROLE_ANONYMOUS")))
        );

        assertThatThrownBy(service::getCurrent)
                .isInstanceOf(UnauthorisedException.class);
    }
}
