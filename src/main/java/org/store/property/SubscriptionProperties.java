package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres métier du module abonnement, externalisés dans `application.yml` sous le préfixe `subscription`.
 */
@ConfigurationProperties(prefix = "subscription")
public record SubscriptionProperties(
        int trialDays
) {
}
