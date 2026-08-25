package org.store.plateforme.application.dto;

import org.store.common.tools.DateHelper;
import org.store.country.application.dto.CountryResponse;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.plateforme.domain.model.DepensePlateforme;

import java.math.BigDecimal;
import java.util.UUID;

public record DepensePlateformeResponse(
        UUID id,
        CategoryDepensePlateformeSummaryResponse category,
        String libelle,
        String description,
        String dateDepense,
        BigDecimal montant,
        MoyenPaiementResponse modePaiement,
        CountryResponse country,
        String createdAt,
        boolean actif
) {
    public DepensePlateformeResponse(DepensePlateforme depense) {
        this(
                depense.getId(),
                depense.getCategory() != null ? new CategoryDepensePlateformeSummaryResponse(depense.getCategory()) : null,
                depense.getLibelle(),
                depense.getDescription(),
                DateHelper.format(depense.getDateDepense()),
                depense.getMontant(),
                depense.getModePaiement() != null ? new MoyenPaiementResponse(depense.getModePaiement()) : null,
                depense.getCountry() != null ? new CountryResponse(depense.getCountry()) : null,
                DateHelper.format(depense.getCreatedAt()),
                depense.isActif()
        );
    }
}
