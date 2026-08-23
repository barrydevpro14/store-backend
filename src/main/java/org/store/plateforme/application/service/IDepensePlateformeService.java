package org.store.plateforme.application.service;

import org.springframework.data.domain.Page;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;

import java.math.BigDecimal;
import java.util.UUID;

public interface IDepensePlateformeService {

    DepensePlateformeResponse create(DepensePlateformeRequest request);

    DepensePlateformeResponse findResponseById(UUID id);

    Page<DepensePlateformeResponse> findAll(DepensePlateformeFilter filter);

    DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter);

    /** Simple period+country sum — consumed by PlateformeReportingServiceImpl, no category/moyen/libelle filters. */
    BigDecimal computeTotal(String startDate, String endDate, UUID countryId);

    DepensePlateformeResponse update(UUID id, DepensePlateformeRequest request);

    void delete(UUID id);
}
