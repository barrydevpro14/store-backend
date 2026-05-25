package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** All KPI counts for the magasin reporting overview — replaces 5 separate API calls. */
public record MagasinOverviewStatsResponse(
        long nombreCommandes,
        BigDecimal totalCommandes,
        BigDecimal totalPaiements,
        BigDecimal ticketMoyen,
        BigDecimal valeurStock,
        long produitsBasSeuil,
        long achatsEnAttente,
        long facturesImpayees
) {
}
