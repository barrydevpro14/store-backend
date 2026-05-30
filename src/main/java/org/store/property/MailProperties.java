package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Custom mail properties bound to {@code app.mail.*}.
 * Spring Boot's standard {@code spring.mail.*} (host, port, username, password)
 * are handled by its auto-configuration; this record covers the additional
 * application-level settings not provided by the standard binding.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(String from) {}
