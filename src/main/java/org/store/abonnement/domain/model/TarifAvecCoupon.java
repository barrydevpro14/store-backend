package org.store.abonnement.domain.model;

/**
 * Projection JPQL associant un tarif actif à son coupon global applicable (null si aucun).
 */
public record TarifAvecCoupon(PlanAbonnementTarif tarif, Coupon coupon) {}
