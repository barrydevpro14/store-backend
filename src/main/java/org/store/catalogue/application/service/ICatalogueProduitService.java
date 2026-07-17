package org.store.catalogue.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.catalogue.application.dto.CatalogueImportResult;
import org.store.catalogue.application.dto.CatalogueProduitFilter;
import org.store.catalogue.application.dto.CatalogueProduitResponse;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.application.dto.CatalogueProduitUpdateRequest;

import java.util.List;
import java.util.UUID;

public interface ICatalogueProduitService {

    List<CatalogueProduitSummaryResponse> findByCurrentEntreprise();

    Page<CatalogueProduitSummaryResponse> findByFilter(CatalogueProduitFilter filter);

    CatalogueImportResult importFromFile(MultipartFile file, UUID activiteEconomiqueId);

    CatalogueProduitResponse update(UUID id, CatalogueProduitUpdateRequest request);

    void delete(UUID id);
}
