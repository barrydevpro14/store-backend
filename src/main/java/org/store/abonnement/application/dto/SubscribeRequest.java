package org.store.abonnement.application.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.UUID;

public record SubscribeRequest(
        @NotNull UUID planId,
        @NotNull UUID typeId,
        @Size(max = 100) String couponCode,
        boolean renouvellementAuto
) {
}
