package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** KPIs vente du magasin filtrés par plage de dates métier, sans données achat. */
public record MagasinVentesStatsResponse(
        long nombreCommandeVentes,
        BigDecimal montantTotalCommandeVentes,
        BigDecimal totalPaiementVentes,
        BigDecimal ticketMoyen,
        long facturesVenteImpayees
) {}
