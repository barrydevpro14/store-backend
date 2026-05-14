package org.store.stock.application.dto;

import org.store.achat.domain.model.CommandeAchat;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;

public record EntreeStockCreate(
        Magasin magasin,
        Product produit,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixAchat,
        String numeroLot,
        LocalDate dateExpiration,
        CommandeAchat commandeAchat
) {
}
