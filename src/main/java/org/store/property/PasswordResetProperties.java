package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "security.password-reset")
public record PasswordResetProperties(int expiryHours) {}
