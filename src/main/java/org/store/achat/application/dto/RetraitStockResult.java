package org.store.achat.application.dto;

import java.math.BigDecimal;

/**
 * Résultat cumulatif d'un retrait de stock à l'annulation d'un achat :
 * quantité totale retirée des lots et nombre de mouvements stock journalisés.
 */
public record RetraitStockResult(BigDecimal totalQuantite, int nombreMouvements) {

    public static RetraitStockResult empty() {
        return new RetraitStockResult(BigDecimal.ZERO, 0);
    }

    public RetraitStockResult merge(RetraitStockResult other) {
        return new RetraitStockResult(
                totalQuantite.add(other.totalQuantite),
                nombreMouvements + other.nombreMouvements
        );
    }
}
