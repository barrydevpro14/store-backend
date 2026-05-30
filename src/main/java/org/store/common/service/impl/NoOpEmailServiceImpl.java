package org.store.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.store.common.service.IEmailService;
import org.store.notification.application.event.ContactMessageRepliedEvent;

/**
 * No-op fallback — created by MailConfig when app.mail.password is blank.
 * Logs a warning and skips sending.
 */
public class NoOpEmailServiceImpl implements IEmailService {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailServiceImpl.class);

    @Override
    public void sendContactReply(ContactMessageRepliedEvent event) {
        log.warn("Email service not configured (app.mail.password missing) — reply to {} not sent", event.email());
    }
}
