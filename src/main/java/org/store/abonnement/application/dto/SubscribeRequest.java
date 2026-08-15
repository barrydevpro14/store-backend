package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotNull;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.common.validation.EnumValue;

import java.util.UUID;

public record SubscribeRequest(
        @NotNull UUID planId,
        @NotNull @EnumValue(enumClass = PeriodiciteAbonnement.class) String periodicite
) {
    public PeriodiciteAbonnement periodiciteAsEnum() {
        return PeriodiciteAbonnement.valueOf(periodicite);
    }
}
