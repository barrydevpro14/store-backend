package org.store.depense.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseParCategorieResponse;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.ICategoryDepenseService;
import org.store.depense.application.service.IDepenseService;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.service.DepenseDomainService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

/**
 * Orchestre le CRUD des dépenses : scoping cross-entité magasin+category, listing filtré
 * et agrégation totale.
 */
@Service
@Transactional(readOnly = true)
public class DepenseServiceImpl implements IDepenseService {

    private final DepenseDomainService depenseDomainService;
    private final IMagasinService magasinService;
    private final ICategoryDepenseService categoryDepenseService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public DepenseServiceImpl(DepenseDomainService depenseDomainService,
                              IMagasinService magasinService,
                              ICategoryDepenseService categoryDepenseService,
                              ICurrentUserService currentUserService,
                              ValidatorService validatorService) {
        this.depenseDomainService = depenseDomainService;
        this.magasinService = magasinService;
        this.categoryDepenseService = categoryDepenseService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /** Crée la dépense après vérification d'accès magasin et d'appartenance category à l'entreprise. */
    @Override
    @Transactional
    public DepenseResponse create(DepenseRequest depenseRequest) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(depenseRequest.magasinId()));
        CategoryDepense category = categoryDepenseService.ensureBelongsToCurrentEntreprise(
                categoryDepenseService.findById(depenseRequest.categoryId()));
        return new DepenseResponse(depenseDomainService.create(depenseRequest, magasin, category));
    }

    /** Retourne la dépense après vérification d'accès magasin. */
    @Override
    public DepenseResponse findResponseById(UUID id) {
        Depense depense = depenseDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(depense.getMagasin());
        return new DepenseResponse(depense);
    }

    @Override
    public Page<DepenseResponse> findAllByCurrentEntreprise(DepenseFilter depenseFilter) {
        validatorService.validate(depenseFilter);
        return depenseDomainService.findResponsesByFilter(depenseFilter, currentUserService.getCurrent().entrepriseId());
    }

    @Override
    public DepenseTotalResponse computeTotal(DepenseFilter depenseFilter) {
        validatorService.validate(depenseFilter);
        return depenseDomainService.computeTotal(depenseFilter, currentUserService.getCurrent().entrepriseId());
    }

    /** Répartition par catégorie sur la période filtrée, triée par montant décroissant. */
    @Override
    public List<DepenseParCategorieResponse> computeByCategory(DepenseFilter depenseFilter) {
        validatorService.validate(depenseFilter);
        return depenseDomainService.computeByCategory(depenseFilter, currentUserService.getCurrent().entrepriseId());
    }

    /** Met à jour la dépense après contrôle d'accès magasin (et magasin/category cible si changés). */
    @Override
    @Transactional
    public DepenseResponse update(UUID id, DepenseRequest depenseRequest) {
        Depense depense = depenseDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(depense.getMagasin());

        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(depenseRequest.magasinId()));
        CategoryDepense category = categoryDepenseService.ensureBelongsToCurrentEntreprise(
                categoryDepenseService.findById(depenseRequest.categoryId()));

        depense.setMagasin(magasin);
        depense.setCategory(category);
        depense.setLibelle(depenseRequest.libelle());
        depense.setDescription(depenseRequest.description());
        depense.setDateDepense(depenseRequest.dateDepense());
        depense.setMontant(depenseRequest.montant());
        depense.setModePaiement(depenseRequest.modePaiement());

        return new DepenseResponse(depenseDomainService.save(depense));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        Depense depense = depenseDomainService.findById(id);
        magasinService.ensureAccessibleByCurrentUser(depense.getMagasin());
        depenseDomainService.delete(depense);
    }
}
