package org.store.achat.application.dto;

/**
 * Résultat cumulatif d'un retrait de stock à l'annulation d'un achat :
 * quantité totale retirée des lots et nombre de mouvements stock journalisés.
 */
public record RetraitStockResult(int totalQuantite, int nombreMouvements) {

    public static RetraitStockResult empty() {
        return new RetraitStockResult(0, 0);
    }

    public RetraitStockResult merge(RetraitStockResult other) {
        return new RetraitStockResult(
                totalQuantite + other.totalQuantite,
                nombreMouvements + other.nombreMouvements
        );
    }
}
