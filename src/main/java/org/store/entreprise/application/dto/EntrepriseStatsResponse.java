package org.store.entreprise.application.dto;

import org.store.entreprise.domain.model.Entreprise;

import java.util.UUID;

/** Per-enterprise stats row for the ADMIN reporting view. */
public record EntrepriseStatsResponse(
        UUID id,
        String sigle,
        String raisonSociale,
        boolean actif,
        boolean trialUsed,
        long magasinCount,
        long employeCount
) {
    public EntrepriseStatsResponse(Entreprise entreprise, long magasinCount, long employeCount) {
        this(
                entreprise.getId(),
                entreprise.getSigle(),
                entreprise.getRaisonSociale(),
                entreprise.isActif(),
                entreprise.isTrialUsed(),
                magasinCount,
                employeCount
        );
    }
}
