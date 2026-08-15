package org.store.abonnement.application.dto;

import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.common.validation.EnumValue;

import java.util.UUID;

/**
 * Requête de changement de plan et/ou de périodicité pour le prochain cycle.
 * Au moins l'un des deux champs doit être non null (contrôlé en service).
 */
public record ChangerPlanRequest(
        UUID planId,
        @EnumValue(enumClass = PeriodiciteAbonnement.class) String periodicite
) {
    public PeriodiciteAbonnement periodiciteAsEnum() {
        return periodicite == null ? null : PeriodiciteAbonnement.valueOf(periodicite);
    }
}
