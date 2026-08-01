package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** KPIs du magasin filtrés par plage de dates métier, pour la page Reporting. */
public record MagasinOverviewStatsResponse(
        long nombreCommandeVentes,
        BigDecimal montantTotalCommandeVentes,
        BigDecimal totalPaiementVentes,
        BigDecimal ticketMoyen,
        long achatsEnAttente,
        long facturesVenteImpayees,
        long facturesAchatImpayees
) {}
