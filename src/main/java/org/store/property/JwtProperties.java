package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "security.jwt")
public record JwtProperties(
        String secret,
        String header,
        String prefix,
        Expiration expiration
) {
    public record Expiration(
            Duration accessToken,
            Duration refreshToken
    ) {
    }
}
