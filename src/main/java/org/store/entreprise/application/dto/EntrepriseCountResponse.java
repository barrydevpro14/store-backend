package org.store.entreprise.application.dto;

/**
 * Nombre total d'entreprises, ventilé par statut actif/inactif. Retourné en une seule requête
 * JPQL — admin overview KPI.
 */
public record EntrepriseCountResponse(long total, long actifs, long inactifs) {}
