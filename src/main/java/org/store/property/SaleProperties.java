package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres métier du module vente, externalisés dans `application.yml` sous le préfixe `sale`.
 * {@code cancelWindowHours} : fenêtre temporelle (en heures) durant laquelle une vente peut être annulée
 * depuis sa création (audité via {@code CommandeVente.createdAt}).
 */
@ConfigurationProperties(prefix = "sale")
public record SaleProperties(
        int cancelWindowHours
) {
}
