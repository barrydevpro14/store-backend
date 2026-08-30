package org.store.paiement.application.service;

import org.springframework.data.domain.Page;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.domain.model.Facturation;

import java.util.List;
import java.util.UUID;

public interface IFacturationService {

    FacturationResponse create(FacturationRequest request);

    FacturationResponse update(UUID id, FacturationRequest request);

    FacturationResponse activate(UUID id);

    FacturationResponse deactivate(UUID id);

    void delete(UUID id);

    FacturationResponse findResponseById(UUID id);

    Page<FacturationResponse> findAll(FacturationFilter filter);

    List<FacturationOptionResponse> findSelectOptions();

    /** Resolves a Facturation by id and enforces it is available for the current user's entreprise country. */
    Facturation findByIdAvailableForCurrentCountry(UUID id);
}
