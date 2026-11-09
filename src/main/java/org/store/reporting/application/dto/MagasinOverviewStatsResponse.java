package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** KPIs du magasin filtrés par plage de dates métier, pour la page Reporting ; benefice / beneficeNet null sans REPORT_FINANCIAL. */
public record MagasinOverviewStatsResponse(
        long nombreCommandeVentes,
        BigDecimal montantTotalCommandeVentes,
        BigDecimal totalPaiementVentes,
        BigDecimal ticketMoyen,
        long achatsEnAttente,
        long facturesVenteImpayees,
        long facturesAchatImpayees,
        BigDecimal montantFacturesVenteImpayees,
        BigDecimal montantFacturesAchatImpayees,
        BigDecimal benefice,
        BigDecimal beneficeNet
) {}
