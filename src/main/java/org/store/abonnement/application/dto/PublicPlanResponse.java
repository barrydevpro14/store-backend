package org.store.abonnement.application.dto;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

public record PublicPlanResponse(
        UUID id,
        String nom,
        String description,
        BigDecimal prix,
        int nombreMagasinsMax,
        int nombreEmployesMax,
        boolean gestionStock,
        boolean gestionVente,
        boolean gestionAchat,
        boolean gestionComptabilite,
        boolean trial,
        int ordre,
        List<PromotionResponse> promotions,
        List<SubscriptionTypeResponse> subscriptionTypes
) {
    /**
     * Constructeur secondaire utilisé par la projection JPQL `SELECT new PublicPlanResponse(plan.id, plan.nom, ...)`.
     * `promotions` et `subscriptionTypes` sont initialisés à des listes vides et attachés ensuite via les wither.
     */
    public PublicPlanResponse(UUID id,
                              String nom,
                              String description,
                              BigDecimal prix,
                              int nombreMagasinsMax,
                              int nombreEmployesMax,
                              boolean gestionStock,
                              boolean gestionVente,
                              boolean gestionAchat,
                              boolean gestionComptabilite,
                              boolean trial,
                              int ordre) {
        this(id, nom, description, prix,
                nombreMagasinsMax, nombreEmployesMax,
                gestionStock, gestionVente, gestionAchat, gestionComptabilite,
                trial, ordre, List.of(), List.of());
    }

    /**
     * Retourne une nouvelle instance avec la liste de promotions remplie (records immutables).
     */
    public PublicPlanResponse withPromotions(List<PromotionResponse> newPromotions) {
        return new PublicPlanResponse(id, nom, description, prix,
                nombreMagasinsMax, nombreEmployesMax,
                gestionStock, gestionVente, gestionAchat, gestionComptabilite,
                trial, ordre, newPromotions, subscriptionTypes);
    }

    /**
     * Retourne une nouvelle instance avec la liste de types (durées) remplie.
     */
    public PublicPlanResponse withSubscriptionTypes(List<SubscriptionTypeResponse> newTypes) {
        return new PublicPlanResponse(id, nom, description, prix,
                nombreMagasinsMax, nombreEmployesMax,
                gestionStock, gestionVente, gestionAchat, gestionComptabilite,
                trial, ordre, promotions, newTypes);
    }
}
