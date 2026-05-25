package org.store.security.application.dto;

import java.math.BigDecimal;

/** All admin reporting overview KPI counts in a single response. */
public record AdminOverviewStatsResponse(
        long totalEntreprises,
        long totalMagasins,
        long totalEmployes,
        long abonnementsActifs,
        long abonnementsTrial,
        long abonnementsExpires,
        long abonnementsSuspendus,
        long paiementsEnAttente,
        long paiementsRejetes,
        BigDecimal revenueYtd
) {
}
