package org.store.common.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.common.service.impl.BrevoApiEmailServiceImpl;
import org.store.property.MailProperties;

/** Active when BREVO_API_KEY is set — bypasses SMTP entirely (HTTPS 443). */
@Component
@Order(1)
public class BrevoApiEmailServiceStrategy implements IEmailServiceStrategy {

    private static final Logger log = LoggerFactory.getLogger(BrevoApiEmailServiceStrategy.class);

    @Override
    public boolean supports(MailProperties props) {
        return props.isBrevoApiConfigured();
    }

    @Override
    public IEmailService create(MailProperties props, IMessageSourceService messageSourceService) {
        log.info("Email service: Brevo HTTP API");
        return new BrevoApiEmailServiceImpl(props.brevoApiKey(), props, messageSourceService);
    }
}
