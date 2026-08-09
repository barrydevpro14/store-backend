package org.store.produit.application.dto;

import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.UniteMesure;

public record ProductCreate(
        ProductRequest request,
        CategoryProduct categoryProduct,
        Entreprise entreprise,
        UniteMesure uniteMesure
) {
}
