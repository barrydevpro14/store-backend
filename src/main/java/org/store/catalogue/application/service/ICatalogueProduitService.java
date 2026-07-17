package org.store.catalogue.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.catalogue.application.dto.CatalogueImportResult;
import org.store.catalogue.application.dto.CatalogueUserFilter;
import org.store.catalogue.application.dto.CatalogueProduitFilter;
import org.store.catalogue.application.dto.CatalogueProduitResponse;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.application.dto.CatalogueProduitUpdateRequest;

import java.util.UUID;

public interface ICatalogueProduitService {

    Page<CatalogueProduitSummaryResponse> findByCurrentEntreprise(CatalogueUserFilter filter);

    Page<CatalogueProduitSummaryResponse> findByFilter(CatalogueProduitFilter filter);

    CatalogueImportResult importFromFile(MultipartFile file, UUID activiteEconomiqueId);

    CatalogueProduitResponse update(UUID id, CatalogueProduitUpdateRequest request);

    void delete(UUID id);
}
