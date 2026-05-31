package org.store.depense.domain.repository;

import java.math.BigDecimal;
import java.util.UUID;

/** Spring Data projection for the native GROUP BY category aggregation. */
public interface DepenseParCategorieProjection {
    UUID getCategoryId();
    String getCategoryNom();
    BigDecimal getMontantTotal();
    Long getNombreDepenses();
}
