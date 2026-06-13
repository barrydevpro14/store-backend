package org.store.common.service.strategy;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.annotation.Order;
import org.springframework.mail.javamail.JavaMailSenderImpl;
import org.springframework.stereotype.Component;
import org.store.common.i18n.IMessageSourceService;
import org.store.common.service.IEmailService;
import org.store.common.service.impl.EmailServiceImpl;
import org.store.property.MailProperties;

import java.util.Properties;

/** Active when app.mail.password is set — sends via SMTP (JavaMailSender). */
@Component
@Order(2)
public class SmtpEmailServiceStrategy implements IEmailServiceStrategy {

    private static final Logger log = LoggerFactory.getLogger(SmtpEmailServiceStrategy.class);

    @Override
    public boolean supports(MailProperties props) {
        return props.isConfigured();
    }

    @Override
    public IEmailService create(MailProperties props, IMessageSourceService messageSourceService) {
        log.info("Email service: SMTP ({}:{})", props.host(), props.port());

        JavaMailSenderImpl sender = new JavaMailSenderImpl();
        sender.setHost(props.host());
        sender.setPort(props.port());
        sender.setUsername(props.username());
        sender.setPassword(props.password());

        Properties javaMailProps = sender.getJavaMailProperties();
        javaMailProps.put("mail.transport.protocol", "smtp");
        javaMailProps.put("mail.smtp.auth", String.valueOf(props.auth()));
        javaMailProps.put("mail.smtp.starttls.enable", String.valueOf(props.starttls()));
        javaMailProps.put("mail.smtp.ssl.enable", String.valueOf(props.ssl()));
        javaMailProps.put("mail.smtp.connectiontimeout", "5000");
        javaMailProps.put("mail.smtp.timeout", "5000");
        javaMailProps.put("mail.smtp.writetimeout", "5000");

        return new EmailServiceImpl(sender, messageSourceService, props);
    }
}
