package org.store.depense.application.service;

import org.springframework.data.domain.Page;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;

import java.util.UUID;

public interface IDepenseService {

    DepenseResponse create(DepenseRequest depenseRequest);

    DepenseResponse findResponseById(UUID id);

    Page<DepenseResponse> findAllByCurrentEntreprise(DepenseFilter depenseFilter);

    DepenseTotalResponse computeTotal(DepenseFilter depenseFilter);

    DepenseResponse update(UUID id, DepenseRequest depenseRequest);

    void delete(UUID id);
}
