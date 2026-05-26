package org.store.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;

/**
 * Fallback email service used when spring.mail.host is not configured.
 * Logs a warning instead of sending — keeps the app functional without SMTP credentials.
 */
@Service
@ConditionalOnMissingBean(IEmailService.class)
public class NoOpEmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailServiceImpl.class);

    @Override
    public void sendContactReply(ContactMessageRepliedEvent event) {
        log.warn("Email service not configured (MAIL_HOST missing) — reply to {} not sent", event.email());
    }
}
