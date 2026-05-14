package org.store.achat.application.dto;

import org.store.achat.domain.model.CommandeAchat;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;

public record LigneCommandeAchatCreate(
        CommandeAchat commande,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixAchat
) {
}
