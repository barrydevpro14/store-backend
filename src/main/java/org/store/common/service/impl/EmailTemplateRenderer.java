package org.store.common.service.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

/** Loads a classpath HTML template and substitutes {{key}} placeholders. */
public final class EmailTemplateRenderer {

    private static final Logger log = LoggerFactory.getLogger(EmailTemplateRenderer.class);

    private EmailTemplateRenderer() {}

    public static String render(String path, Map<String, String> vars) {
        try {
            String template = new ClassPathResource(path)
                    .getContentAsString(StandardCharsets.UTF_8);
            for (Map.Entry<String, String> entry : vars.entrySet()) {
                template = template.replace("{{" + entry.getKey() + "}}", entry.getValue());
            }
            return template;
        } catch (IOException e) {
            log.error("Email template not found: {}", path);
            return vars.getOrDefault("reply", "");
        }
    }
}
