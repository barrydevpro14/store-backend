package org.store.inventaire.application.dto;

import org.store.common.tools.DateHelper;
import org.store.inventaire.domain.enums.StatutRapport;
import org.store.inventaire.domain.model.RapportInventaire;

import java.math.BigDecimal;
import java.util.UUID;

public record RapportInventaireResponse(
        UUID id,
        UUID inventaireId,
        BigDecimal montantAutomatique,
        BigDecimal montantPhysique,
        BigDecimal ecart,
        BigDecimal montantCaisse,
        BigDecimal depense,
        BigDecimal montantRoulement,
        String dateDebutPeriode,
        String dateFinPeriode,
        BigDecimal benefice,
        StatutRapport status,
        String createdAt
) {
    public RapportInventaireResponse(RapportInventaire rapport) {
        this(
                rapport.getId(),
                rapport.getInventaire().getId(),
                rapport.getMontantAutomatique(),
                rapport.getMontantPhysique(),
                rapport.getEcart(),
                rapport.getMontantCaisse(),
                rapport.getDepense(),
                rapport.getMontantRoulement(),
                DateHelper.format(rapport.getDateDebutPeriode()),
                DateHelper.format(rapport.getDateFinPeriode()),
                rapport.getBenefice(),
                rapport.getStatus(),
                DateHelper.format(rapport.getCreatedAt())
        );
    }
}
