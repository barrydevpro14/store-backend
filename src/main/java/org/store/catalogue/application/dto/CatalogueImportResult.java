package org.store.catalogue.application.dto;

import java.util.List;

public record CatalogueImportResult(
        int imported,
        int ignored,
        List<String> errors
) {
}
