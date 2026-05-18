package org.store.vente.application.dto;

/**
 * Résultat cumulatif d'une ré-injection de stock à l'annulation d'une vente :
 * quantité totale recréditée et nombre de mouvements stock journalisés.
 */
public record ReinjectionStockResult(int totalQuantite, int nombreMouvements) {

    public static ReinjectionStockResult empty() {
        return new ReinjectionStockResult(0, 0);
    }

    public ReinjectionStockResult merge(ReinjectionStockResult other) {
        return new ReinjectionStockResult(
                totalQuantite + other.totalQuantite,
                nombreMouvements + other.nombreMouvements
        );
    }
}
