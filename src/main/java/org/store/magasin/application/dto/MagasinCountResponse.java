package org.store.magasin.application.dto;

/**
 * Nombre de magasins de l'entreprise du caller, ventilé par statut actif/inactif.
 * Retourné par {@code GET /api/v1/magasins/count} en une seule requête JPQL.
 */
public record MagasinCountResponse(long total, long actifs, long inactifs) {}
