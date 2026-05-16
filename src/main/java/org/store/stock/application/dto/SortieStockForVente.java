package org.store.stock.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.vente.domain.model.LigneCommandeVente;

import java.math.BigDecimal;

public record SortieStockForVente(
        Magasin magasin,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixVente,
        LigneCommandeVente ligneVente
) {
}
