package org.store.reporting.application.dto;

/** Snapshot global des KPIs de l'entreprise pour le dashboard OWNER, sans filtre de date. */
public record OwnerOverviewStatsResponse(
        long produitsBasSeuil,
        long achatsEnAttente,
        long facturesVenteImpayees,
        long facturesAchatImpayees
) {}
