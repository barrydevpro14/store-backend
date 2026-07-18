package org.store.vente.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.repository.query.Param;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.dto.DataCountResponse;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.domain.model.FactureClient;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IFactureClientService {

    Page<FactureClientResponse> findAllByCurrentEntreprise(FactureClientFilter filter);

    DataCountResponse countAllUnpaid(UUID magasingId);

    FactureClient findById(UUID id);

    FactureClientResponse findResponseById(UUID id);

    List<FactureClient> findDueOnDates(@Param("dates") List<LocalDate> dates , List<StatutFacture> statutFactures);
}
