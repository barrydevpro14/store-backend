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
        List<PromotionResponse> promotions
) {
    /**
     * Constructeur secondaire utilisé par la projection JPQL `SELECT new PublicPlanResponse(plan.id, plan.nom, ...)`.
     * Les promotions sont initialisées à une liste vide et attachées ensuite via `withPromotions(...)`.
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
                trial, ordre, List.of());
    }

    /**
     * Retourne une nouvelle instance avec la liste de promotions remplie (records immutables).
     */
    public PublicPlanResponse withPromotions(List<PromotionResponse> newPromotions) {
        return new PublicPlanResponse(id, nom, description, prix,
                nombreMagasinsMax, nombreEmployesMax,
                gestionStock, gestionVente, gestionAchat, gestionComptabilite,
                trial, ordre, newPromotions);
    }
}
