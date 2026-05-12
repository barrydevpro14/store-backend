package org.store.entreprise.application.dto;

import org.store.entreprise.domain.model.Entreprise;

import java.util.UUID;

public record EntrepriseResponse(
        UUID id,
        String sigle,
        String raisonSociale,
        String ninea,
        String rccm,
        String adresse,
        boolean actif,
        boolean trialUsed
) {
    public EntrepriseResponse(Entreprise entreprise) {
        this(
                entreprise.getId(),
                entreprise.getSigle(),
                entreprise.getRaisonSociale(),
                entreprise.getNinea(),
                entreprise.getRccm(),
                entreprise.getAdresse(),
                entreprise.isActif(),
                entreprise.isTrialUsed()
        );
    }
}
