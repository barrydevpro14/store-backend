package org.store.security.application.service;

import org.store.security.application.service.impl.JwtServiceImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.property.JwtProperties;
import org.store.security.application.dto.UserPrincipal;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class JwtServiceImplTest {

    private JwtServiceImpl service;

    @BeforeEach
    void setUp() {
        String secret = "0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        JwtProperties properties = new JwtProperties(
                secret,
                "Authorization",
                "Bearer ",
                new JwtProperties.Expiration(Duration.ofHours(1), Duration.ofDays(7))
        );
        service = new JwtServiceImpl(properties);
    }

    @Test
    void should_generate_non_blank_token_for_user_principal() {
        UserPrincipal principal = samplePrincipal();

        String token = service.generateToken(principal);

        assertThat(token).isNotBlank();
        assertThat(token.split("\\.")).hasSize(3);
    }

    @Test
    void should_validate_freshly_generated_token() {
        String token = service.generateToken(samplePrincipal());

        assertThat(service.isTokenValid(token)).isTrue();
    }

    @Test
    void should_reject_invalid_token() {
        assertThat(service.isTokenValid("not.a.valid.jwt")).isFalse();
    }

    @Test
    void should_extract_user_principal_from_token() {
        UserPrincipal original = samplePrincipal();
        String token = service.generateToken(original);

        UserPrincipal extracted = service.extractUserPrincipal(token);

        assertThat(extracted.userId()).isEqualTo(original.userId());
        assertThat(extracted.entrepriseId()).isEqualTo(original.entrepriseId());
        assertThat(extracted.magasinId()).isEqualTo(original.magasinId());
        assertThat(extracted.username()).isEqualTo(original.username());
        assertThat(extracted.role()).isEqualTo(original.role());
        assertThat(extracted.permissions()).containsExactlyElementsOf(original.permissions());
    }

    private UserPrincipal samplePrincipal() {
        return new UserPrincipal(
                UUID.randomUUID(),
                UUID.randomUUID(),
                UUID.randomUUID(),
                "john.doe",
                "PROPRIETAIRE",
                List.of("PROPRIETAIRE_ACCESS")
        );
    }
}
