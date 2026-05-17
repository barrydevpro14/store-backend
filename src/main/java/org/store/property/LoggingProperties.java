package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres opérationnels du filtre de logging HTTP, externalisés dans `application.yml`
 * sous le préfixe `logging.http`. Permet d'ajuster la verbosité entre dev/staging/prod.
 */
@ConfigurationProperties(prefix = "logging.http")
public record LoggingProperties(
        int maxPayloadLength
) {
}
