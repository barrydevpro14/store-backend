package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;
import org.store.notification.application.event.EmployeWelcomeEvent;
import org.store.notification.application.event.OwnerWelcomeEvent;
import org.store.notification.application.event.PasswordResetRequestedEvent;

/**
 * Listens to business events that require outbound emails and delegates to IEmailService.
 * All handlers run @Async — email failures never block the originating transaction.
 */
@Component
public class EmailEventListener {

    private static final Logger log = LoggerFactory.getLogger(EmailEventListener.class);

    private final IEmailService emailService;

    public EmailEventListener(IEmailService emailService) {
        this.emailService = emailService;
    }

    @Async
    @EventListener
    public void onContactMessageReplied(ContactMessageRepliedEvent event) {
        emailService.sendContactReply(event);
        log.info("ContactMessageReplied email dispatched to {}", event.email());
    }

    @Async
    @EventListener
    public void onPasswordResetRequested(PasswordResetRequestedEvent event) {
        emailService.sendPasswordReset(event.toEmail(), event.recipientName(), event.resetLink());
        log.info("PasswordReset email dispatched to {}", event.toEmail());
    }

    @Async
    @EventListener
    public void onEmployeWelcome(EmployeWelcomeEvent event) {
        emailService.sendWelcomeEmploye(event.toEmail(), event.recipientName(), event.username(), event.password());
        log.info("EmployeWelcome email dispatched to {}", event.toEmail());
    }

    @Async
    @EventListener
    public void onOwnerWelcome(OwnerWelcomeEvent event) {
        emailService.sendWelcomeOwner(event.toEmail(), event.recipientName(), event.username(), event.entrepriseName(), event.loginUrl());
        log.info("OwnerWelcome email dispatched to {}", event.toEmail());
    }
}
