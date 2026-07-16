package org.store.entreprise.application.dto;

import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.country.application.dto.CountryResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.presentation.EntrepriseController;
import org.store.users.application.dto.ProprietaireSummaryResponse;

import java.util.UUID;

public record EntrepriseDetailResponse(
        UUID id,
        String sigle,
        String raisonSociale,
        String ninea,
        String rccm,
        String adresse,
        String telephone,
        CountryResponse country,
        boolean actif,
        boolean trialUsed,
        String logo,
        ProprietaireSummaryResponse proprietaire,
        ActiviteEconomiqueSummaryResponse activiteEconomique
) {
    public EntrepriseDetailResponse(Entreprise entreprise) {
        this(
                entreprise.getId(),
                entreprise.getSigle(),
                entreprise.getRaisonSociale(),
                entreprise.getNinea(),
                entreprise.getRccm(),
                entreprise.getAdresse(),
                entreprise.getTelephone(),
                entreprise.getCountry() != null ? new CountryResponse(entreprise.getCountry()) : null,
                entreprise.isActif(),
                entreprise.isTrialUsed(),
                entreprise.getLogo() != null ? EntrepriseController.BASE_PATH + "/me/logo" : null,
                entreprise.getProprietaire() != null ? new ProprietaireSummaryResponse(entreprise.getProprietaire()) : null,
                entreprise.getActiviteEconomique() != null
                        ? new ActiviteEconomiqueSummaryResponse(entreprise.getActiviteEconomique()) : null
        );
    }
}
