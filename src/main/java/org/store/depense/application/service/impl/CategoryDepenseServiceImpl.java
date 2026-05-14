package org.store.depense.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.depense.application.dto.CategoryDepenseRequest;
import org.store.depense.application.dto.CategoryDepenseResponse;
import org.store.depense.application.service.ICategoryDepenseService;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.service.CategoryDepenseDomainService;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * CRUD des catégories de dépense, scopé par entreprise du caller.
 */
@Service
@Transactional(readOnly = true)
public class CategoryDepenseServiceImpl implements ICategoryDepenseService {

    private final CategoryDepenseDomainService categoryDepenseDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;

    public CategoryDepenseServiceImpl(CategoryDepenseDomainService categoryDepenseDomainService,
                                      IEntrepriseService entrepriseService,
                                      ICurrentUserService currentUserService) {
        this.categoryDepenseDomainService = categoryDepenseDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
    }

    /** Crée la catégorie pour l'entreprise du caller après vérification d'unicité du nom. */
    @Override
    @Transactional
    public CategoryDepenseResponse create(CategoryDepenseRequest categoryDepenseRequest) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        ensureNomAvailable(categoryDepenseRequest.nom(), entrepriseId);
        Entreprise entreprise = entrepriseService.findById(entrepriseId);
        return new CategoryDepenseResponse(categoryDepenseDomainService.create(categoryDepenseRequest, entreprise));
    }

    @Override
    public CategoryDepense findById(UUID id) {
        return categoryDepenseDomainService.findById(id);
    }

    @Override
    public CategoryDepenseResponse findResponseById(UUID id) {
        return new CategoryDepenseResponse(ensureBelongsToCurrentEntreprise(categoryDepenseDomainService.findById(id)));
    }

    @Override
    public Page<CategoryDepenseResponse> findAllByCurrentEntreprise(Pageable pageable) {
        return categoryDepenseDomainService.findResponsesByEntrepriseId(currentUserService.getCurrent().entrepriseId(), pageable);
    }

    /** Met à jour la catégorie après contrôle d'appartenance et unicité du nom (si changé). */
    @Override
    @Transactional
    public CategoryDepenseResponse update(UUID id, CategoryDepenseRequest categoryDepenseRequest) {
        CategoryDepense category = ensureBelongsToCurrentEntreprise(categoryDepenseDomainService.findById(id));
        if (!category.getNom().equals(categoryDepenseRequest.nom())) {
            ensureNomAvailable(categoryDepenseRequest.nom(), category.getEntreprise().getId());
        }
        category.setNom(categoryDepenseRequest.nom());
        category.setDescription(categoryDepenseRequest.description());
        if (categoryDepenseRequest.actif() != null) {
            category.setActif(categoryDepenseRequest.actif());
        }
        return new CategoryDepenseResponse(categoryDepenseDomainService.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        CategoryDepense category = ensureBelongsToCurrentEntreprise(categoryDepenseDomainService.findById(id));
        categoryDepenseDomainService.delete(category);
    }

    /** Lève ForbiddenException si la catégorie n'appartient pas à l'entreprise du caller. */
    @Override
    public CategoryDepense ensureBelongsToCurrentEntreprise(CategoryDepense categoryDepense) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!categoryDepense.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("categoryDepense.notOwned");
        }
        return categoryDepense;
    }

    /** Lève UniqueResourceException si une catégorie portant ce nom existe déjà dans l'entreprise. */
    public void ensureNomAvailable(String nom, UUID entrepriseId) {
        if (categoryDepenseDomainService.existsByNomAndEntrepriseId(nom, entrepriseId)) {
            throw new UniqueResourceException("categoryDepense.nom.alreadyExists", nom);
        }
    }
}
