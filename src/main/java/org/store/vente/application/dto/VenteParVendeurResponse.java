package org.store.vente.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record VenteParVendeurResponse(
        UUID vendeurId,
        String nomComplet,
        long nombreCommandes,
        BigDecimal totalCommandes
) {
}
