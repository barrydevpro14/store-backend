package org.store.produit.application.dto;

import java.math.BigDecimal;
import java.util.UUID;

/** Résultat plat de recherche produit-variante pour le sélecteur de vente — une ligne = un ProductFournisseur avec stock actif. */
public record ProductVariantSearchResponse(
        UUID value,
        UUID productId,
        UUID qualityId,
        UUID fournisseurId,
        String label,
        BigDecimal prixAchat,
        BigDecimal prixVente
) {
    /** Constructeur appelé par JPQL — reçoit le label de base et la quantité agrégée, construit le label complet. */
    public ProductVariantSearchResponse(UUID value, UUID productId, UUID qualityId, UUID fournisseurId,
                                        String labelBase, BigDecimal prixAchat, BigDecimal prixVente,
                                        Long quantiteEnStock) {
        this(value, productId, qualityId, fournisseurId,
                labelBase + " (" + quantiteEnStock + ")",
                prixAchat, prixVente);
    }
}
