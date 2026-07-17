package org.store.catalogue.application.dto;

import java.util.UUID;

public record CatalogueProduitSummaryResponse(
        UUID id,
        String reference,
        String libelle,
        String categorie,
        String description
) {
}
