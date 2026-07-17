package org.store.catalogue.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.catalogue.application.dto.CatalogueProduitFilter;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.domain.model.CatalogueProduit;
import org.store.catalogue.domain.repository.CatalogueProduitRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.UUID;

@Service
public class CatalogueProduitDomainService extends GlobalService<CatalogueProduit, CatalogueProduitRepository> {

    public CatalogueProduitDomainService(CatalogueProduitRepository repository) {
        super(repository);
    }

    public boolean existsByReferenceAndLibelleAndActiviteEconomiqueId(String reference, String libelle, UUID activiteEconomiqueId) {
        return repository.existsByReferenceAndLibelleAndActiviteEconomiqueId(reference, libelle, activiteEconomiqueId);
    }

    public List<CatalogueProduitSummaryResponse> findSummariesByActiviteEconomiqueId(UUID activiteEconomiqueId) {
        return repository.findSummariesByActiviteEconomiqueId(activiteEconomiqueId);
    }

    public Page<CatalogueProduitSummaryResponse> findByFilter(CatalogueProduitFilter filter) {
        return repository.findByFilter(
                filter.activiteEconomiqueId(),
                filter.reference(),
                filter.referencePattern(),
                filter.libelle(),
                filter.libellePattern(),
                filter.categorie(),
                filter.categoriePattern(),
                filter.createdStartDate(),
                filter.createdEndDate(),
                filter.toPageable()
        );
    }
}
