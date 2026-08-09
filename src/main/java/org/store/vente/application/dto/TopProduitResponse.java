package org.store.vente.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record TopProduitResponse(
        UUID productId,
        String nom,
        String reference,
        BigDecimal quantiteVendue,
        BigDecimal chiffreAffaires,
        String uniteMesure
) {
}
