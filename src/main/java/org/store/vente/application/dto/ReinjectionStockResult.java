package org.store.vente.application.dto;

import java.math.BigDecimal;

/**
 * Résultat cumulatif d'une ré-injection de stock à l'annulation d'une vente :
 * quantité totale recréditée et nombre de mouvements stock journalisés.
 */
public record ReinjectionStockResult(BigDecimal totalQuantite, int nombreMouvements) {

    public static ReinjectionStockResult empty() {
        return new ReinjectionStockResult(BigDecimal.ZERO, 0);
    }

    public ReinjectionStockResult merge(ReinjectionStockResult other) {
        return new ReinjectionStockResult(
                totalQuantite.add(other.totalQuantite),
                nombreMouvements + other.nombreMouvements
        );
    }
}
