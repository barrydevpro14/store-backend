package org.store.paiement.application.dto;

import org.store.country.application.dto.CountryResponse;
import org.store.paiement.domain.model.Facturation;

import java.util.List;
import java.util.UUID;

public record FacturationResponse(
        UUID id,
        MoyenPaiementResponse moyenPaiement,
        List<CountryResponse> pays,
        String numeroFacturation,
        boolean actif
) {
    public FacturationResponse(Facturation facturation) {
        this(
                facturation.getId(),
                new MoyenPaiementResponse(facturation.getMoyenPaiement()),
                facturation.getPays().stream().map(CountryResponse::new).toList(),
                facturation.getNumeroFacturation(),
                facturation.isActif()
        );
    }
}
