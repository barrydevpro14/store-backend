package org.store.stock.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;

import java.math.BigDecimal;

/** Context record for a stock entry operation: target magasin, product, quantity, and purchase price. */
public record StockEntryContext(
        Magasin magasin,
        Product produit,
        int quantite,
        BigDecimal prixAchat
) {
}
