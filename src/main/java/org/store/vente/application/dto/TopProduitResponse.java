package org.store.vente.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProduitResponse(
        UUID productId,
        String nom,
        String reference,
        BigDecimal quantiteVendue,
        BigDecimal chiffreAffaires,
        String uniteMesure,
        BigDecimal benefice
) {
    /** Copie sans le bénéfice, pour les utilisateurs sans REPORT_FINANCIAL. */
    public TopProduitResponse withoutBenefice() {
        return new TopProduitResponse(productId, nom, reference, quantiteVendue, chiffreAffaires, uniteMesure, null);
    }
}
