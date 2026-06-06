package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;

import java.util.UUID;

public interface IFactureClientService {

    Page<FactureClientResponse> findAllByCurrentEntreprise(FactureClientFilter filter);

    FactureClientResponse findResponseById(UUID id);
}
