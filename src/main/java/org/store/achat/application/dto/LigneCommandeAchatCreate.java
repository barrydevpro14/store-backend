package org.store.achat.application.dto;

import org.store.achat.domain.model.CommandeAchat;
import org.store.produit.domain.model.ProductFournisseur;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LigneCommandeAchatCreate(
        CommandeAchat commande,
        ProductFournisseur productFournisseur,
        int quantite,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        String numeroLot,
        LocalDate dateExpiration
) {
}
