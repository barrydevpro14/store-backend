package org.store.achat.application.dto;

import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.List;

public record AchatContext(
        AchatRequest request,
        Magasin magasin,
        CommandeAchat commande,
        FactureAchat facture,
        List<ProductFournisseur> productFournisseurs
) {
}
