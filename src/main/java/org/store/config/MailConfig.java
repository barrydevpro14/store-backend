package org.store.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.common.service.impl.EmailServiceImpl;
import org.store.common.service.impl.NoOpEmailServiceImpl;
import org.store.property.MailProperties;

import java.util.Properties;

/**
 * Creates either a live EmailServiceImpl (when app.mail.password is set)
 * or a NoOpEmailServiceImpl fallback.
 * All conditions live here — @ConditionalOnBean/@ConditionalOnMissingBean on
 * @Service classes are unreliable in component-scanned packages.
 */
@Configuration
public class MailConfig {

    private static final Logger log = LoggerFactory.getLogger(MailConfig.class);

    @Bean
    public IEmailService emailService(MailProperties mail, IMessageSourceService messageSourceService) {
        if (!mail.isConfigured()) {
            log.warn("Email service not configured (app.mail.password is blank) — NoOpEmailService active");
            return new NoOpEmailServiceImpl();
        }

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(mail.host());
        sender.setPort(mail.port());
        sender.setUsername(mail.username());
        sender.setPassword(mail.password());

        Properties props = sender.getJavaMailProperties();
        props.put("mail.transport.protocol", "smtp");
        props.put("mail.smtp.auth", String.valueOf(mail.auth()));
        props.put("mail.smtp.starttls.enable", String.valueOf(mail.starttls()));
        props.put("mail.smtp.connectiontimeout", "5000");
        props.put("mail.smtp.timeout", "5000");
        props.put("mail.smtp.writetimeout", "5000");

        return new EmailServiceImpl(sender, messageSourceService, mail);
    }
}
