package org.store.common.service.strategy;

import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.property.MailProperties;

/** Strategy that knows when it applies and how to build an IEmailService. */
public interface IEmailServiceStrategy {

    boolean supports(MailProperties props);

    IEmailService create(MailProperties props, IMessageSourceService messageSourceService);
}
