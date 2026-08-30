package org.store.paiement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.dto.DataSelect;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.service.MoyenPaiementDomainService;

import java.util.HashSet;
import java.util.List;
import java.util.UUID;

/**
 * Gère le CRUD des moyens de paiement globaux (ADMIN uniquement).
 */
@Service
@Transactional(readOnly = true)
public class MoyenPaiementServiceImpl implements IMoyenPaiementService {

    private final MoyenPaiementDomainService domainService;
    private final ValidatorService validatorService;
    private final CountryDomainService countryDomainService;
    private final IEntrepriseService entrepriseService;

    public MoyenPaiementServiceImpl(MoyenPaiementDomainService domainService,
                                    ValidatorService validatorService,
                                    CountryDomainService countryDomainService,
                                    IEntrepriseService entrepriseService) {
        this.domainService = domainService;
        this.validatorService = validatorService;
        this.countryDomainService = countryDomainService;
        this.entrepriseService = entrepriseService;
    }

    /** Retourne tous les moyens de paiement (actifs et inactifs). */
    @Override
    public List<MoyenPaiementResponse> findAll() {
        return domainService.findAll().stream()
                .map(MoyenPaiementResponse::new)
                .toList();
    }

    /** Retourne l'entité par id — utilisée par les autres services pour résoudre l'UUID. */
    @Override
    public MoyenPaiement findById(UUID id) {
        return domainService.findById(id);
    }

    /**
     * Listing filtré pour combobox. Si aucun {@code countryId} n'est fourni,
     * résout le pays de l'entreprise de l'utilisateur courant.
     */
    @Override
    public Page<DataSelect> findSelectItems(MoyenPaiementSelectFilter filter) {
        validatorService.validate(filter);

        UUID resolvedCountryId = filter.countryId() != null ? filter.countryId() : entrepriseService.findCurrentUserCountryId();
        MoyenPaiementSelectFilter effectiveFilter = new MoyenPaiementSelectFilter(resolvedCountryId, filter.searchTerm(), filter.page(), filter.size());

        return domainService.findSelectItems(effectiveFilter);
    }

    /** Crée un nouveau moyen de paiement et résout ses pays depuis paysIds, après vérification d'unicité du libellé. */
    @Override
    @Transactional
    public MoyenPaiementResponse create(MoyenPaiementRequest request) {
        validatorService.validate(request);
        ensureLibelleUnique(request.libelle(), null);

        MoyenPaiement moyen = new MoyenPaiement();
        moyen.setLibelle(request.libelle());
        moyen.setCode(request.libelle().toUpperCase().replaceAll("[^A-Z0-9]", "_"));
        moyen.setPays(new HashSet<>(countryDomainService.findAllByIds(request.paysIds())));

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Met à jour le libellé et les pays d'un moyen de paiement existant. */
    @Override
    @Transactional
    public MoyenPaiementResponse update(UUID id, MoyenPaiementRequest request) {
        validatorService.validate(request);
        MoyenPaiement moyen = domainService.findById(id);
        ensureLibelleUnique(request.libelle(), id);

        moyen.setLibelle(request.libelle());
        moyen.setPays(new HashSet<>(countryDomainService.findAllByIds(request.paysIds())));

        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Active un moyen de paiement désactivé. */
    @Override
    @Transactional
    public MoyenPaiementResponse activate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(true);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Désactive un moyen de paiement (soft-disable — garde l'historique). */
    @Override
    @Transactional
    public MoyenPaiementResponse deactivate(UUID id) {
        MoyenPaiement moyen = domainService.findById(id);
        moyen.setActif(false);
        return new MoyenPaiementResponse(domainService.save(moyen));
    }

    /** Supprime définitivement un moyen de paiement. */
    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    /** Vérifie que le libellé n'est pas déjà utilisé par un autre moyen de paiement. */
    private void ensureLibelleUnique(String libelle, UUID excludeId) {
        boolean conflict = domainService.findAll().stream()
                .anyMatch(m -> m.getLibelle().equalsIgnoreCase(libelle)
                        && (excludeId == null || !m.getId().equals(excludeId)));
        if (conflict) {
            throw new BadArgumentException("moyenPaiement.libelle.alreadyExists", libelle);
        }
    }
}
