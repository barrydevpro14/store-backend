package org.store.common.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.common.service.impl.NoOpEmailServiceImpl;
import org.store.property.MailProperties;

/** Fallback — always applicable, silently discards all emails. */
@Component
@Order(3)
public class NoOpEmailServiceStrategy implements IEmailServiceStrategy {

    private static final Logger log = LoggerFactory.getLogger(NoOpEmailServiceStrategy.class);

    @Override
    public boolean supports(MailProperties props) {
        return true;
    }

    @Override
    public IEmailService create(MailProperties props, IMessageSourceService messageSourceService) {
        log.warn("Email service: NoOp — emails will be silently discarded");
        return new NoOpEmailServiceImpl();
    }
}
