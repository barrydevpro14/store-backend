package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;

import java.util.UUID;

public interface IPaiementVenteService {

    Page<PaiementVenteResponse> findByFactureId(UUID factureId, Pageable pageable);

    PaiementVenteResponse create(UUID factureId, PaiementVenteRequest paiementVenteRequest);
}
