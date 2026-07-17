package org.store.common.dto;

public record ExcelProductRow(
        String reference,
        String libelle,
        String description,
        String categorie
) {
}
