package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * All outbound SMTP settings consolidated under {@code app.mail.*}.
 * Default values are declared in {@code email.yml} (imported by application.yml).
 * Override at runtime via env vars: MAIL_HOST, MAIL_PORT, MAIL_USERNAME,
 * MAIL_PASSWORD (required to activate email sending), MAIL_FROM.
 *
 * <p>When {@code password} is blank, MailConfig does not create the
 * JavaMailSender bean and NoOpEmailServiceImpl handles sends silently.
 */
@ConfigurationProperties(prefix = "app.mail")
public record MailProperties(
        String host,
        int port,
        String username,
        String password,
        String from,
        boolean auth,
        boolean starttls,
        boolean ssl
) {
    public boolean isConfigured() {
        return password != null && !password.isBlank();
    }
}
