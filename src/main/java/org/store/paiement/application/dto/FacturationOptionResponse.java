package org.store.paiement.application.dto;

import java.util.UUID;

public record FacturationOptionResponse(
        UUID facturationId,
        String moyenLibelle,
        String numeroFacturation
) {
}
