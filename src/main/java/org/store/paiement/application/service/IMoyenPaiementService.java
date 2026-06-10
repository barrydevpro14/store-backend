package org.store.paiement.application.service;

import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.UUID;

public interface IMoyenPaiementService {

    List<MoyenPaiementResponse> findAll();

    MoyenPaiement findById(UUID id);

    MoyenPaiementResponse create(MoyenPaiementRequest request);

    MoyenPaiementResponse update(UUID id, MoyenPaiementRequest request);

    MoyenPaiementResponse activate(UUID id);

    MoyenPaiementResponse deactivate(UUID id);

    void delete(UUID id);
}
