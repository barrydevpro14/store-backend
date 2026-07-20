package org.store.activite.application.service;

import org.store.activite.application.dto.ActiviteEconomiqueRequest;
import org.store.activite.application.dto.ActiviteEconomiqueResponse;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;

import java.util.List;
import java.util.UUID;

public interface IActiviteEconomiqueService {

    ActiviteEconomiqueResponse create(ActiviteEconomiqueRequest request);

    ActiviteEconomiqueResponse findResponseById(UUID id);

    ActiviteEconomiqueResponse update(UUID id, ActiviteEconomiqueRequest request);

    ActiviteEconomiqueResponse activate(UUID id);

    ActiviteEconomiqueResponse deactivate(UUID id);

    void delete(UUID id);

    List<ActiviteEconomiqueResponse> findAll();

    List<ActiviteEconomiqueSummaryResponse> findAllActive();
}
