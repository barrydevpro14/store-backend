package org.store.paiement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.FacturationDomainService;

import java.util.List;
import java.util.UUID;

/**
 * Orchestrates the Facturation CRUD: resolves the moyenPaiement/pays FKs, enforces the
 * unique (moyenPaiement, pays) pair invariant, and delegates persistence to the domain service.
 */
@Service
@Transactional(readOnly = true)
public class FacturationServiceImpl implements IFacturationService {

    private final FacturationDomainService domainService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;
    private final IEntrepriseService entrepriseService;

    public FacturationServiceImpl(FacturationDomainService domainService,
                                  IMoyenPaiementService moyenPaiementService,
                                  CountryDomainService countryDomainService,
                                  ValidatorService validatorService,
                                  IEntrepriseService entrepriseService) {
        this.domainService = domainService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
        this.entrepriseService = entrepriseService;
    }

    /** Validates the request, checks the moyenPaiement/pays pair is unique, resolves both FKs, and creates the facturation. */
    @Override
    @Transactional
    public FacturationResponse create(FacturationRequest request) {
        validatorService.validate(request);
        domainService.ensureUniqueMoyenPaysPair(request.moyenPaiementId(), request.paysId(), null);

        MoyenPaiement moyenPaiement = resolveMoyenPaiement(request.moyenPaiementId());
        Country pays = resolvePays(request.paysId());
        Facturation created = domainService.create(request, moyenPaiement, pays);

        return new FacturationResponse(created);
    }

    /** Validates the request, checks the moyenPaiement/pays pair stays unique excluding the current id, then updates the facturation. */
    @Override
    @Transactional
    public FacturationResponse update(UUID id, FacturationRequest request) {
        validatorService.validate(request);
        Facturation facturation = domainService.findById(id);
        domainService.ensureUniqueMoyenPaysPair(request.moyenPaiementId(), request.paysId(), id);

        MoyenPaiement moyenPaiement = resolveMoyenPaiement(request.moyenPaiementId());
        Country pays = resolvePays(request.paysId());

        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());

        return new FacturationResponse(domainService.save(facturation));
    }

    /** Resolves the mandatory moyenPaiement FK from its id. */
    private MoyenPaiement resolveMoyenPaiement(UUID moyenPaiementId) {
        return moyenPaiementService.findById(moyenPaiementId);
    }

    /** Resolves the optional pays FK from its id, returning null when the request carries no paysId. */
    private Country resolvePays(UUID paysId) {
        return paysId != null ? countryDomainService.findById(paysId) : null;
    }

    /** Activates a disabled facturation. */
    @Override
    @Transactional
    public FacturationResponse activate(UUID id) {
        Facturation facturation = domainService.findById(id);
        facturation.setActif(true);
        return new FacturationResponse(domainService.save(facturation));
    }

    /** Deactivates a facturation. */
    @Override
    @Transactional
    public FacturationResponse deactivate(UUID id) {
        Facturation facturation = domainService.findById(id);
        facturation.setActif(false);
        return new FacturationResponse(domainService.save(facturation));
    }

    /** Permanently deletes the facturation. */
    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    /** Returns the facturation matching the given id. */
    @Override
    public FacturationResponse findResponseById(UUID id) {
        return new FacturationResponse(domainService.findById(id));
    }

    /** Validates the filter, then delegates the paginated lookup to the domain service. */
    @Override
    public Page<FacturationResponse> findAll(FacturationFilter filter) {
        validatorService.validate(filter);
        return domainService.findResponsesByFilter(filter);
    }

    /** Resolves the caller's entreprise country, then returns the matching active facturation options (global + country-specific). */
    @Override
    public List<FacturationOptionResponse> findSelectOptions() {
        UUID countryId = entrepriseService.findCurrentUserCountryId();
        return domainService.findSelectOptions(countryId);
    }

    /** Resolves the facturation by id, enforcing it belongs to the current user's entreprise country when it is country-specific. */
    @Override
    public Facturation findByIdAvailableForCurrentCountry(UUID id) {
        Facturation facturation = domainService.findById(id);
        Country pays = facturation.getPays();
        if (pays != null) {
            UUID currentCountryId = entrepriseService.findCurrentUserCountryId();
            if (!pays.getId().equals(currentCountryId)) {
                throw new BadArgumentException("facturation.notAvailableForCountry");
            }
        }
        return facturation;
    }
}
