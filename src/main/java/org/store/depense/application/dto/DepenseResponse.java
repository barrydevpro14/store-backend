package org.store.depense.application.dto;

import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.tools.DateHelper;
import org.store.depense.domain.model.Depense;
import org.store.magasin.application.dto.MagasinSummaryResponse;

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
        MoyenPaiement modePaiement,
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
                depense.getModePaiement(),
                DateHelper.format(depense.getCreatedAt())
        );
    }
}
