package org.store.depense.application.dto;

import org.store.common.tools.DateHelper;
import org.store.depense.domain.model.Depense;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;

import java.math.BigDecimal;
import java.util.UUID;

public record DepenseResponse(
        UUID id,
        MagasinSummaryResponse magasin,
        CategoryDepenseSummaryResponse category,
        String libelle,
        String description,
        String dateDepense,
        BigDecimal montant,
        MoyenPaiementResponse modePaiement,
        String createdAt
) {
    public DepenseResponse(Depense depense) {
        this(
                depense.getId(),
                depense.getMagasin() != null ? new MagasinSummaryResponse(depense.getMagasin()) : null,
                depense.getCategory() != null ? new CategoryDepenseSummaryResponse(depense.getCategory()) : null,
                depense.getLibelle(),
                depense.getDescription(),
                DateHelper.format(depense.getDateDepense()),
                depense.getMontant(),
                depense.getModePaiement() != null ? new MoyenPaiementResponse(depense.getModePaiement()) : null,
                DateHelper.format(depense.getCreatedAt())
        );
    }
}
