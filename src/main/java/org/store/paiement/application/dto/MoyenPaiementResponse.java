package org.store.paiement.application.dto;

import org.store.country.application.dto.CountryResponse;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.UUID;

public record MoyenPaiementResponse(
        UUID id,
        String libelle,
        boolean actif,
        List<CountryResponse> pays
) {
    public MoyenPaiementResponse(UUID id, String libelle, boolean actif) {
        this(id, libelle, actif, List.of());
    }

    public MoyenPaiementResponse(MoyenPaiement moyenPaiement) {
        this(
                moyenPaiement.getId(),
                moyenPaiement.getLibelle(),
                moyenPaiement.isActif(),
                moyenPaiement.getPays().stream().map(CountryResponse::new).toList()
        );
    }
}
