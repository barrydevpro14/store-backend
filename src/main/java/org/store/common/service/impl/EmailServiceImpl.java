package org.store.common.service.impl;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.property.MailProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/**
 * Live email service — created by MailConfig when app.mail.password is set.
 * Renders HTML from templates/email/*.html (simple {{placeholder}} substitution).
 */
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);
    private static final String TEMPLATE_PATH = "templates/email/contact-reply.html";
    private static final String RESET_TEMPLATE_PATH = "templates/email/password-reset.html";
    private static final String WELCOME_TEMPLATE_PATH = "templates/email/employee-welcome.html";

    private final JavaMailSender mailSender;
    private final IMessageSourceService messageSourceService;
    private final MailProperties mailProperties;

    public EmailServiceImpl(JavaMailSender mailSender,
                            IMessageSourceService messageSourceService,
                            MailProperties mailProperties) {
        this.mailSender = mailSender;
        this.messageSourceService = messageSourceService;
        this.mailProperties = mailProperties;
    }

    @Override
    public void sendContactReply(ContactMessageRepliedEvent event) {
        java.util.Locale locale = event.locale();
        String subject = messageSourceService.getMessage(
                "email.contact.reply.subject", new Object[]{event.sujet()}, locale);

        String html = renderTemplate(TEMPLATE_PATH, Map.of(
                "subject",          subject,
                "appName",          messageSourceService.getMessage("email.appName", null, locale),
                "greeting",         messageSourceService.getMessage("email.contact.reply.greeting", new Object[]{event.nom()}, locale),
                "intro",            messageSourceService.getMessage("email.contact.reply.intro", null, locale),
                "labelYourMessage", messageSourceService.getMessage("email.contact.reply.yourMessage", null, locale),
                "originalMessage",  event.originalMessage().replace("\n", "<br/>"),
                "labelOurReply",    messageSourceService.getMessage("email.contact.reply.ourReply", null, locale),
                "reply",            event.reponse().replace("\n", "<br/>"),
                "footer",           messageSourceService.getMessage("email.contact.reply.footer", null, locale)
        ));

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(event.email());
            helper.setSubject(subject);
            helper.setText(html, true);

            mailSender.send(mime);
            log.info("Contact reply email sent to {}", event.email());
        } catch (MailException | MessagingException e) {
            log.error("Failed to send contact reply email to {}: {}", event.email(), e.getMessage());
        }
    }

    @Override
    public void sendPasswordReset(String toEmail, String recipientName, String resetLink) {
        java.util.Locale locale = java.util.Locale.FRENCH;
        String subject = messageSourceService.getMessage("email.passwordReset.subject", null, locale);
        String appName = messageSourceService.getMessage("email.appName", null, locale);

        String html = renderTemplate(RESET_TEMPLATE_PATH, Map.of(
                "subject",   subject,
                "appName",   appName,
                "greeting",  messageSourceService.getMessage("email.passwordReset.greeting",  new Object[]{recipientName}, locale),
                "intro",     messageSourceService.getMessage("email.passwordReset.intro",     null, locale),
                "btnLabel",  messageSourceService.getMessage("email.passwordReset.btnLabel",  null, locale),
                "resetLink", resetLink,
                "expiry",    messageSourceService.getMessage("email.passwordReset.expiry",    null, locale),
                "noRequest", messageSourceService.getMessage("email.passwordReset.noRequest", null, locale),
                "footer",    messageSourceService.getMessage("email.passwordReset.footer",    new Object[]{appName}, locale)
        ));

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Password reset email sent to {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send password reset email to {}: {}", toEmail, e.getMessage());
        }
    }

    @Override
    public void sendWelcomeEmploye(String toEmail, String recipientName, String username, String password) {
        java.util.Locale locale = java.util.Locale.FRENCH;
        String subject = messageSourceService.getMessage("email.employeWelcome.subject", null, locale);
        String appName = messageSourceService.getMessage("email.appName", null, locale);

        String html = renderTemplate(WELCOME_TEMPLATE_PATH, Map.of(
                "subject",           subject,
                "appName",           appName,
                "greeting",          messageSourceService.getMessage("email.employeWelcome.greeting",          new Object[]{recipientName}, locale),
                "intro",             messageSourceService.getMessage("email.employeWelcome.intro",             new Object[]{appName},       locale),
                "labelUsername",     messageSourceService.getMessage("email.employeWelcome.labelUsername",     null, locale),
                "username",          username,
                "labelPassword",     messageSourceService.getMessage("email.employeWelcome.labelPassword",     null, locale),
                "password",          password,
                "changePasswordNote",messageSourceService.getMessage("email.employeWelcome.changePasswordNote",null, locale),
                "footer",            messageSourceService.getMessage("email.passwordReset.footer",             new Object[]{appName}, locale)
        ));

        try {
            MimeMessage mime = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mime, false, "UTF-8");
            helper.setFrom(mailProperties.from());
            helper.setTo(toEmail);
            helper.setSubject(subject);
            helper.setText(html, true);
            mailSender.send(mime);
            log.info("Welcome email sent to new employee {}", toEmail);
        } catch (MailException | MessagingException e) {
            log.error("Failed to send welcome email to {}: {}", toEmail, e.getMessage());
        }
    }

    /** Loads a classpath HTML template and replaces all {{key}} placeholders. */
    private String renderTemplate(String path, Map<String, String> vars) {
        try {
            String template = new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return template;
        } catch (IOException e) {
            log.error("Email template not found: {}", path);
            return vars.getOrDefault("reply", "");
        }
    }
}
