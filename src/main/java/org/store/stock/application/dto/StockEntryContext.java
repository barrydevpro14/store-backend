package org.store.stock.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;

/** Context record for a stock entry: target magasin, ProductFournisseur, quantity, and purchase price. */
public record StockEntryContext(
        Magasin magasin,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixAchat
) {
}
