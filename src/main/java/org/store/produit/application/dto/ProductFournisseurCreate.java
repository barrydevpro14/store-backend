package org.store.produit.application.dto;

import org.store.achat.domain.model.Fournisseur;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.Quality;

/** Command record for creating a ProductFournisseur: aggregates the request DTO and the three resolved entities. */
public record ProductFournisseurCreate(
        ProductFournisseurRequest productFournisseurRequest,
        Product product,
        Fournisseur fournisseur,
        Quality quality
) {
}
