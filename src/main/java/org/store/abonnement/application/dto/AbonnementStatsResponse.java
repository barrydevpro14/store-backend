package org.store.abonnement.application.dto;

/**
 * Nombre d'abonnements par statut (actifs / trial / expirés / suspendus), en une seule requête
 * JPQL — admin overview KPI.
 */
public record AbonnementStatsResponse(long actifs, long trial, long expires, long suspendus) {}
