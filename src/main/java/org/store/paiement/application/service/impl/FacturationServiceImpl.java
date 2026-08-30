package org.store.paiement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.FacturationDomainService;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Orchestrates the Facturation CRUD: resolves the moyenPaiement/pays FKs, enforces the
 * no-country-overlap invariant per moyen, and delegates persistence to the domain service.
 */
@Service
@Transactional(readOnly = true)
public class FacturationServiceImpl implements IFacturationService {

    private final FacturationDomainService domainService;
    private final IMoyenPaiementService moyenPaiementService;
    private final CountryDomainService countryDomainService;
    private final ValidatorService validatorService;

    public FacturationServiceImpl(FacturationDomainService domainService,
                                  IMoyenPaiementService moyenPaiementService,
                                  CountryDomainService countryDomainService,
                                  ValidatorService validatorService) {
        this.domainService = domainService;
        this.moyenPaiementService = moyenPaiementService;
        this.countryDomainService = countryDomainService;
        this.validatorService = validatorService;
    }

    /** Validates the request, checks the moyen's countries don't overlap another facturation, resolves both FKs, and creates the facturation. */
    @Override
    @Transactional
    public FacturationResponse create(FacturationRequest request) {
        validatorService.validate(request);
        domainService.ensureNoCountryOverlap(request.moyenPaiementId(), request.paysIds(), null);

        MoyenPaiement moyenPaiement = resolveMoyenPaiement(request.moyenPaiementId());
        Set<Country> pays = new HashSet<>(countryDomainService.findAllByIds(request.paysIds()));
        Facturation created = domainService.create(request, moyenPaiement, pays);

        return new FacturationResponse(created);
    }

    /** Validates the request, checks the moyen's countries stay non-overlapping excluding the current id, then updates the facturation. */
    @Override
    @Transactional
    public FacturationResponse update(UUID id, FacturationRequest request) {
        validatorService.validate(request);
        Facturation facturation = domainService.findById(id);
        domainService.ensureNoCountryOverlap(request.moyenPaiementId(), request.paysIds(), id);

        MoyenPaiement moyenPaiement = resolveMoyenPaiement(request.moyenPaiementId());
        Set<Country> pays = new HashSet<>(countryDomainService.findAllByIds(request.paysIds()));

        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());

        return new FacturationResponse(domainService.save(facturation));
    }

    /** Resolves the mandatory moyenPaiement FK from its id. */
    public MoyenPaiement resolveMoyenPaiement(UUID moyenPaiementId) {
        return moyenPaiementService.findById(moyenPaiementId);
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

    /** Returns the active facturation options (global + country-specific) available for the given country. */
    @Override
    public List<FacturationOptionResponse> findSelectOptions(UUID countryId) {
        return domainService.findSelectOptions(countryId);
    }

    /** Resolves the facturation by id, enforcing it is active and, when country-specific, its pays set contains the given country. */
    @Override
    public Facturation findByIdAvailableForCountry(UUID id, UUID countryId) {
        Facturation facturation = domainService.findById(id);
        if (!facturation.isActif()) {
            throw new BadArgumentException("facturation.notAvailable");
        }

        Set<Country> pays = facturation.getPays();
        boolean isRestrictedToOtherCountries = !pays.isEmpty()
                && pays.stream().noneMatch(country -> country.getId().equals(countryId));
        if (isRestrictedToOtherCountries) {
            throw new BadArgumentException("facturation.notAvailableForCountry");
        }
        return facturation;
    }
}
