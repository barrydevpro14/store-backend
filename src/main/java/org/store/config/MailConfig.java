package org.store.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.common.service.impl.NoOpEmailServiceImpl;
import org.store.common.service.strategy.IEmailServiceStrategy;
import org.store.property.MailProperties;

import java.util.List;

/**
 * Selects the active IEmailService by delegating to the first applicable
 * IEmailServiceStrategy (ordered by @Order). No conditionals here —
 * each strategy encapsulates its own applicability check.
 */
@Configuration
public class MailConfig {

    @Bean
    public IEmailService emailService(MailProperties mail,
                                      IMessageSourceService messageSourceService,
                                      List<IEmailServiceStrategy> strategies) {
        return strategies.stream()
                .filter(s -> s.supports(mail))
                .findFirst()
                .map(s -> s.create(mail, messageSourceService))
                .orElseGet(NoOpEmailServiceImpl::new);
    }
}
