package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** Snapshot global des KPIs du magasin, sans filtre de date, pour le dashboard home. */
public record MagasinDashboardStatsResponse(
        BigDecimal valeurStock,
        long produitsBasSeuil,
        long achatsEnAttente,
        long facturesVenteImpayees,
        long facturesAchatImpayees
) {}
