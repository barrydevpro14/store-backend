package org.store.catalogue.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.activite.domain.model.ActiviteEconomique;
import org.store.activite.domain.service.ActiviteEconomiqueDomainService;
import org.store.catalogue.application.dto.CatalogueImportResult;
import org.store.catalogue.application.dto.CatalogueProduitFilter;
import org.store.catalogue.application.dto.CatalogueProduitResponse;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.application.dto.CatalogueProduitUpdateRequest;
import org.store.catalogue.application.service.ICatalogueProduitService;
import org.store.catalogue.domain.model.CatalogueProduit;
import org.store.catalogue.domain.service.CatalogueProduitDomainService;
import org.store.common.dto.ExcelParseResult;
import org.store.common.dto.ExcelProductRow;
import org.store.common.service.IExcelProductRowService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.security.application.service.ICurrentUserService;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Gestion du catalogue Bhantic : consultation par entreprise,
 * import bulk Excel et mise à jour/suppression par l'ADMIN.
 */
@Service
public class CatalogueProduitServiceImpl implements ICatalogueProduitService {

    private final CatalogueProduitDomainService catalogueProduitDomainService;
    private final ActiviteEconomiqueDomainService activiteEconomiqueDomainService;
    private final ICurrentUserService currentUserService;
    private final IEntrepriseService entrepriseService;
    private final IExcelProductRowService excelProductRowService;

    public CatalogueProduitServiceImpl(
            CatalogueProduitDomainService catalogueProduitDomainService,
            ActiviteEconomiqueDomainService activiteEconomiqueDomainService,
            ICurrentUserService currentUserService,
            IEntrepriseService entrepriseService,
            IExcelProductRowService excelProductRowService
    ) {
        this.catalogueProduitDomainService = catalogueProduitDomainService;
        this.activiteEconomiqueDomainService = activiteEconomiqueDomainService;
        this.currentUserService = currentUserService;
        this.entrepriseService = entrepriseService;
        this.excelProductRowService = excelProductRowService;
    }

    @Override
    public List<CatalogueProduitSummaryResponse> findByCurrentEntreprise() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        UUID activiteEconomiqueId = entrepriseService.findById(entrepriseId).getActiviteEconomique().getId();

        return catalogueProduitDomainService.findSummariesByActiviteEconomiqueId(activiteEconomiqueId);
    }

    @Override
    public Page<CatalogueProduitSummaryResponse> findByFilter(CatalogueProduitFilter filter) {
        return catalogueProduitDomainService.findByFilter(filter);
    }

    @Override
    @Transactional
    public CatalogueImportResult importFromFile(MultipartFile multipartFile, UUID activiteEconomiqueId) {
        ActiviteEconomique activite = activiteEconomiqueDomainService.findById(activiteEconomiqueId);
        ExcelParseResult parseResult = excelProductRowService.parseRows(multipartFile);

        int imported = 0;
        int ignored = 0;
        List<String> errors = new ArrayList<>(parseResult.errors());

        for (ExcelProductRow excelRow : parseResult.rows()) {
            try {
                if (catalogueProduitDomainService.existsByReferenceAndLibelleAndActiviteEconomiqueId(
                        excelRow.reference(), excelRow.libelle(), activiteEconomiqueId)) {
                    ignored++;
                    continue;
                }

                CatalogueProduit entry = new CatalogueProduit();
                entry.setActiviteEconomique(activite);
                entry.setReference(excelRow.reference());
                entry.setLibelle(excelRow.libelle());
                entry.setDescription(excelRow.description());
                entry.setCategorie(excelRow.categorie());

                catalogueProduitDomainService.save(entry);
                imported++;

            } catch (Exception exception) {
                errors.add(excelRow.reference() + " : " + exception.getMessage());
            }
        }

        return new CatalogueImportResult(imported, ignored, errors);
    }

    @Override
    @Transactional
    public CatalogueProduitResponse update(UUID id, CatalogueProduitUpdateRequest request) {
        CatalogueProduit catalogue = catalogueProduitDomainService.findById(id);
        catalogue.setReference(request.reference());
        catalogue.setLibelle(request.libelle());
        catalogue.setCategorie(request.categorie());
        catalogue.setDescription(request.description());

        return new CatalogueProduitResponse(catalogueProduitDomainService.save(catalogue));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        catalogueProduitDomainService.deleteById(id);
    }

}
