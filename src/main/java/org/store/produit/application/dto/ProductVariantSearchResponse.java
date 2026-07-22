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
    /** Constructeur appelé par JPQL depuis Stock.quantiteDisponible (int). */
    public ProductVariantSearchResponse(UUID value, UUID productId, UUID qualityId, UUID fournisseurId,
                                        String labelBase, BigDecimal prixAchat, BigDecimal prixVente,
                                        Integer quantiteEnStock) {
        this(value, productId, qualityId, fournisseurId,
                labelBase + " (" + quantiteEnStock + ")",
                prixAchat, prixVente);
    }
}
