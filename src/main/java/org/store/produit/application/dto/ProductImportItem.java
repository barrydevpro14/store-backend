package org.store.produit.application.dto;

public record ProductImportItem(
        String reference,
        String libelle,
        String description,
        String categorie
) {
}
