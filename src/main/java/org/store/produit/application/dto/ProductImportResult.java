package org.store.produit.application.dto;

import java.util.List;

public record ProductImportResult(
        int produitsImportes,
        int produitsIgnores,
        int categoriesCreees,
        List<ProductImportError> erreurs
) {
}
