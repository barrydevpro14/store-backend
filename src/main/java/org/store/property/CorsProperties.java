package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

/**
 * Allowed CORS origins loaded from {@code cors.allowed-origins}.
 * Default values are declared in {@code allowed_ip.yml} (optional, gitignored).
 * Override at runtime via env var: {@code CORS_ALLOWED_ORIGINS=url1,url2}.
 *
 * <p>Each entry must be an exact origin (scheme + host + optional port).
 * Example: {@code https://myapp.vercel.app}. Wildcards are not supported.
 */
@ConfigurationProperties(prefix = "cors")
public record CorsProperties(List<String> allowedOrigins) {

    public boolean isAllowed(String origin) {
        if (origin == null || origin.isBlank()) return false;
        return allowedOrigins != null && allowedOrigins.contains(origin);
    }
}
