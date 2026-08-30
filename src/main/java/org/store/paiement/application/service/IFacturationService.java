package org.store.paiement.application.service;

import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;

import java.util.UUID;

public interface IFacturationService {

    FacturationResponse create(FacturationRequest request);

    FacturationResponse update(UUID id, FacturationRequest request);

    FacturationResponse activate(UUID id);

    FacturationResponse deactivate(UUID id);

    void delete(UUID id);

    FacturationResponse findResponseById(UUID id);
}
