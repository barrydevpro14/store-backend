package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** All admin reporting overview KPI counts in a single response. */
public record AdminOverviewStatsResponse(
        long totalEntreprises,
        long totalEntreprisesActives,
        long totalEntreprisesInactives,
        long totalMagasins,
        long totalMagasinsActifs,
        long totalMagasinsInactifs,
        long totalEmployes,
        long abonnementsActifs,
        long abonnementsTrial,
        long abonnementsExpires,
        long abonnementsSuspendus,
        long paiementsEnAttente,
        long paiementsRejetes,
        long contactMessagesNouveaux,
        BigDecimal revenueYtd
) {
}
