package org.store.vente.application.dto;

import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.users.domain.model.Employe;
import org.store.vente.domain.model.CommandeVente;

import java.util.List;

public record VenteContext(
        VenteRequest request,
        CommandeVente commande,
        Magasin magasin,
        Employe user,
        List<ProductFournisseur> productFournisseurs
) {
}
