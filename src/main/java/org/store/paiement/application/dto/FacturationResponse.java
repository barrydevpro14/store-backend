package org.store.paiement.application.dto;

import org.store.country.application.dto.CountryResponse;
import org.store.paiement.domain.model.Facturation;

import java.util.UUID;

public record FacturationResponse(
        UUID id,
        MoyenPaiementResponse moyenPaiement,
        CountryResponse pays,
        String numeroFacturation,
        boolean actif
) {
    public FacturationResponse(Facturation facturation) {
        this(
                facturation.getId(),
                new MoyenPaiementResponse(facturation.getMoyenPaiement()),
                facturation.getPays() != null ? new CountryResponse(facturation.getPays()) : null,
                facturation.getNumeroFacturation(),
                facturation.isActif()
        );
    }
}
