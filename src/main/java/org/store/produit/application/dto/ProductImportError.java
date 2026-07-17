package org.store.produit.application.dto;

public record ProductImportError(
        String reference,
        String libelle,
        String message
) {
}
