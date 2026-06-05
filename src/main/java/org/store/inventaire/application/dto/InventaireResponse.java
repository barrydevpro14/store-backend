package org.store.inventaire.application.dto;

import org.store.common.tools.DateHelper;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.model.Inventaire;
import org.store.magasin.application.dto.MagasinSummaryResponse;

import java.time.LocalDate;
import java.util.UUID;

public record InventaireResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        InventaireStatut statut,
        LocalDate date,
        String dateValidation,
        String commentaire,
        String createdAt
) {
    public InventaireResponse(Inventaire inventaire) {
        this(
                inventaire.getId(),
                new MagasinSummaryResponse(inventaire.getMagasin()),
                inventaire.getStatut(),
                inventaire.getDate(),
                DateHelper.format(inventaire.getDateValidation()),
                inventaire.getCommentaire(),
                DateHelper.format(inventaire.getCreatedAt())
        );
    }
}
