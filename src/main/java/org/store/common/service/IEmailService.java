package org.store.common.service;

import org.store.notification.application.event.ContactMessageRepliedEvent;

/** Outbound email operations. Implementations are conditional on SMTP configuration. */
public interface IEmailService {
    void sendContactReply(ContactMessageRepliedEvent event);
}
