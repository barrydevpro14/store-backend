package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.application.service.IDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.service.DepensePlateformeDomainService;

import java.math.BigDecimal;
import java.util.UUID;

/** Orchestre le CRUD des dépenses plateforme : résolution FK category/moyen/country et agrégation totale. */
@Service
@Transactional(readOnly = true)
public class DepensePlateformeServiceImpl implements IDepensePlateformeService {

    private final DepensePlateformeDomainService domainService;
    private final ICategoryDepensePlateformeService categoryService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;

    public DepensePlateformeServiceImpl(DepensePlateformeDomainService domainService,
                                        ICategoryDepensePlateformeService categoryService,
                                        IMoyenPaiementService moyenPaiementService,
                                        CountryDomainService countryDomainService,
                                        ValidatorService validatorService) {
        this.domainService = domainService;
        this.categoryService = categoryService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
    }

    @Override
    @Transactional
    public DepensePlateformeResponse create(DepensePlateformeRequest request) {
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;
        return new DepensePlateformeResponse(domainService.create(request, category, moyen, country));
    }

    @Override
    public DepensePlateformeResponse findResponseById(UUID id) {
        return new DepensePlateformeResponse(domainService.findById(id));
    }

    @Override
    public Page<DepensePlateformeResponse> findAll(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.findResponsesByFilter(filter);
    }

    @Override
    public DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter) {
        validatorService.validate(filter);
        return domainService.computeTotal(filter);
    }

    @Override
    public BigDecimal computeTotal(String startDate, String endDate, UUID countryId) {
        return domainService.sumByPeriod(startDate, endDate, countryId);
    }

    @Override
    @Transactional
    public DepensePlateformeResponse update(UUID id, DepensePlateformeRequest request) {
        DepensePlateforme depense = domainService.findById(id);
        CategoryDepensePlateforme category = categoryService.findById(request.categoryId());
        MoyenPaiement moyen = moyenPaiementService.findById(request.moyenPaiementId());
        Country country = request.countryId() != null ? countryDomainService.findById(request.countryId()) : null;

        depense.setCategory(category);
        depense.setLibelle(request.libelle());
        depense.setDescription(request.description());
        depense.setDateDepense(request.dateDepense());
        depense.setMontant(request.montant());
        depense.setModePaiement(moyen);
        depense.setCountry(country);

        return new DepensePlateformeResponse(domainService.save(depense));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }
}
