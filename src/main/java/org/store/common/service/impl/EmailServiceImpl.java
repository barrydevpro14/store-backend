package org.store.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.property.MailProperties;

/**
 * Sends transactional emails via JavaMailSender.
 * Only active when spring.mail.host is configured.
 * MailException is caught and logged — failures never propagate to the caller.
 */
@Service
@ConditionalOnProperty(name = "spring.mail.host")
public class EmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(EmailServiceImpl.class);

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
        String subject = messageSourceService.getMessage(
                "email.contact.reply.subject", new Object[]{event.sujet()});

        String body = messageSourceService.getMessage(
                "email.contact.reply.body",
                new Object[]{event.nom(), event.sujet(), event.originalMessage(), event.reponse()});

        SimpleMailMessage mail = buildMessage(event.email(), subject, body);

        try {
            mailSender.send(mail);
            log.info("Contact reply email sent to {}", event.email());
        } catch (MailException e) {
            log.error("Failed to send contact reply email to {}: {}", event.email(), e.getMessage());
        }
    }

    private SimpleMailMessage buildMessage(String to, String subject, String body) {
        SimpleMailMessage mail = new SimpleMailMessage();
        mail.setFrom(mailProperties.from());
        mail.setTo(to);
        mail.setSubject(subject);
        mail.setText(body);
        return mail;
    }
}
