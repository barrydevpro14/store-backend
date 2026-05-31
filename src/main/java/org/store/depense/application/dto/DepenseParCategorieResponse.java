package org.store.depense.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Agrégat dépenses groupées par catégorie, trié par montant décroissant. */
public record DepenseParCategorieResponse(
        UUID categoryId,
        String categoryNom,
        BigDecimal montantTotal,
        long nombreDepenses
) {
    public DepenseParCategorieResponse(UUID categoryId, String categoryNom,
                                       BigDecimal montantTotal, Long nombreDepenses) {
        this(categoryId, categoryNom,
                montantTotal != null ? montantTotal : BigDecimal.ZERO,
                nombreDepenses != null ? nombreDepenses : 0L);
    }
}
