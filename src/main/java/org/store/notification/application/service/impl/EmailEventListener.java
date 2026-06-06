package org.store.notification.application.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;

/**
 * Listens to business events that require outbound emails and delegates to IEmailService.
 * Runs asynchronously — email failures never block the originating transaction.
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
}
