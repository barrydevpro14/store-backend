package org.store.vente.application.dto;

import org.store.produit.domain.model.ProductFournisseur;
import org.store.vente.domain.model.CommandeVente;

import java.math.BigDecimal;

public record LigneCommandeVenteCreate(
        CommandeVente commande,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixUnitaire
) {
}
