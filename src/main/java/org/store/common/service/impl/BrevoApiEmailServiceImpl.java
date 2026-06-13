package org.store.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.client.RestTemplate;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.property.MailProperties;

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * Email service using Brevo Transactional Email HTTP API (v3).
 * Created by MailConfig when app.mail.brevo-api-key is set.
 * Bypasses SMTP entirely — uses HTTPS port 443, unblocked on Railway.
 */
public class BrevoApiEmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiEmailServiceImpl.class);
    private static final String BREVO_API_URL = "https://api.brevo.com/v3/smtp/email";

    private static final String TEMPLATE_PATH        = "templates/email/contact-reply.html";
    private static final String RESET_TEMPLATE_PATH  = "templates/email/password-reset.html";
    private static final String WELCOME_TEMPLATE_PATH       = "templates/email/employee-welcome.html";
    private static final String OWNER_WELCOME_TEMPLATE_PATH = "templates/email/owner-welcome.html";

    private final String apiKey;
    private final MailProperties mailProperties;
    private final IMessageSourceService messageSourceService;
    private final RestTemplate restTemplate;

    public BrevoApiEmailServiceImpl(String apiKey,
                                    MailProperties mailProperties,
                                    IMessageSourceService messageSourceService) {
        this.apiKey = apiKey;
        this.mailProperties = mailProperties;
        this.messageSourceService = messageSourceService;
        this.restTemplate = new RestTemplate();
    }

    @Override
    public void sendContactReply(ContactMessageRepliedEvent event) {
        Locale locale = event.locale();
        String subject = messageSourceService.getMessage(
                "email.contact.reply.subject", new Object[]{event.sujet()}, locale);

        String html = EmailTemplateRenderer.render(TEMPLATE_PATH, Map.of(
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

        send(event.email(), subject, html, "contact reply");
    }

    @Override
    public void sendPasswordReset(String toEmail, String recipientName, String resetLink) {
        Locale locale = Locale.FRENCH;
        String subject = messageSourceService.getMessage("email.passwordReset.subject", null, locale);
        String appName = messageSourceService.getMessage("email.appName", null, locale);

        String html = EmailTemplateRenderer.render(RESET_TEMPLATE_PATH, Map.of(
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

        send(toEmail, subject, html, "password reset");
    }

    @Override
    public void sendWelcomeEmploye(String toEmail, String recipientName, String username, String password) {
        Locale locale = Locale.FRENCH;
        String subject = messageSourceService.getMessage("email.employeWelcome.subject", null, locale);
        String appName = messageSourceService.getMessage("email.appName", null, locale);

        String html = EmailTemplateRenderer.render(WELCOME_TEMPLATE_PATH, Map.of(
                "subject",            subject,
                "appName",            appName,
                "greeting",           messageSourceService.getMessage("email.employeWelcome.greeting",           new Object[]{recipientName}, locale),
                "intro",              messageSourceService.getMessage("email.employeWelcome.intro",               new Object[]{appName}, locale),
                "labelUsername",      messageSourceService.getMessage("email.employeWelcome.labelUsername",       null, locale),
                "username",           username,
                "labelPassword",      messageSourceService.getMessage("email.employeWelcome.labelPassword",       null, locale),
                "password",           password,
                "changePasswordNote", messageSourceService.getMessage("email.employeWelcome.changePasswordNote",  null, locale),
                "footer",             messageSourceService.getMessage("email.passwordReset.footer",               new Object[]{appName}, locale)
        ));

        send(toEmail, subject, html, "employee welcome");
    }

    @Override
    public void sendWelcomeOwner(String toEmail, String recipientName, String username, String entrepriseName, String loginUrl) {
        Locale locale = Locale.FRENCH;
        String subject = messageSourceService.getMessage("email.ownerWelcome.subject", new Object[]{entrepriseName}, locale);
        String appName = messageSourceService.getMessage("email.appName", null, locale);

        Map<String, String> vars = new HashMap<>();
        vars.put("subject",        subject);
        vars.put("appName",        appName);
        vars.put("entrepriseName", entrepriseName);
        vars.put("greeting",       messageSourceService.getMessage("email.ownerWelcome.greeting",      new Object[]{recipientName}, locale));
        vars.put("intro",          messageSourceService.getMessage("email.ownerWelcome.intro",         new Object[]{appName}, locale));
        vars.put("labelUsername",  messageSourceService.getMessage("email.ownerWelcome.labelUsername", null, locale));
        vars.put("username",       username);
        vars.put("ctaText",        messageSourceService.getMessage("email.ownerWelcome.ctaText",       null, locale));
        vars.put("btnLabel",       messageSourceService.getMessage("email.ownerWelcome.btnLabel",      null, locale));
        vars.put("loginUrl",       loginUrl);
        vars.put("footer",         messageSourceService.getMessage("email.passwordReset.footer",       new Object[]{appName}, locale));

        String html = EmailTemplateRenderer.render(OWNER_WELCOME_TEMPLATE_PATH, vars);
        send(toEmail, subject, html, "owner welcome");
    }

    private void send(String toEmail, String subject, String html, String type) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set("api-key", apiKey);

            Map<String, Object> body = Map.of(
                    "sender",      Map.of("email", mailProperties.from()),
                    "to",          List.of(Map.of("email", toEmail)),
                    "subject",     subject,
                    "htmlContent", html
            );

            restTemplate.postForObject(BREVO_API_URL, new HttpEntity<>(body, headers), String.class);
            log.info("Brevo API: {} email sent to {}", type, toEmail);
        } catch (Exception e) {
            log.error("Brevo API: failed to send {} email to {}: {}", type, toEmail, e.getMessage());
        }
    }
}
