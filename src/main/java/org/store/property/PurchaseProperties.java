package org.store.property;

import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Paramètres métier du module achat, externalisés dans `application.yml` sous le préfixe `purchase`.
 * {@code cancelWindowHours} : fenêtre temporelle (en heures) durant laquelle une commande d'achat
 * réceptionnée peut être annulée depuis sa création (audité via {@code CommandeAchat.createdAt}).
 */
@ConfigurationProperties(prefix = "purchase")
public record PurchaseProperties(
        int cancelWindowHours
) {
}
