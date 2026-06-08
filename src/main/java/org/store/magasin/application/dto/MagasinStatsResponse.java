package org.store.magasin.application.dto;

import java.math.BigDecimal;

/**
 * Statistiques d'un magasin : employés, clients, stock et revenu mensuel.
 * Accessible à l'OWNER et au MANAGER du magasin (permission STORE_READ_ONE).
 */
public record MagasinStatsResponse(
        long nombreEmployes,
        long nombreClients,
        long nombreProduitsEnStock,
        BigDecimal valeurTotaleStock,
        BigDecimal revenuMoisCourant
) {}
