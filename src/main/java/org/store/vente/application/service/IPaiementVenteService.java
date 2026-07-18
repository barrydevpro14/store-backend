package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;

import java.util.List;
import java.util.UUID;

public interface IPaiementVenteService {

    List<PaiementVenteResponse> findAllByFactureId(UUID factureId);

    Page<PaiementVenteResponse> findByFactureId(UUID factureId, Pageable pageable);

    PaiementVenteResponse create(UUID factureId, PaiementVenteRequest paiementVenteRequest);
}
