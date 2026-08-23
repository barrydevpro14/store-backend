package org.store.plateforme.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UniqueResourceException;
import org.store.plateforme.application.dto.CategoryDepensePlateformeFilter;
import org.store.plateforme.application.dto.CategoryDepensePlateformeRequest;
import org.store.plateforme.application.dto.CategoryDepensePlateformeResponse;
import org.store.plateforme.application.service.ICategoryDepensePlateformeService;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.service.CategoryDepensePlateformeDomainService;

import java.util.UUID;

/** CRUD des catégories de dépense plateforme — référentiel global, non scopé par entreprise. */
@Service
@Transactional(readOnly = true)
public class CategoryDepensePlateformeServiceImpl implements ICategoryDepensePlateformeService {

    private final CategoryDepensePlateformeDomainService domainService;

    public CategoryDepensePlateformeServiceImpl(CategoryDepensePlateformeDomainService domainService) {
        this.domainService = domainService;
    }

    /** Crée la catégorie après vérification d'unicité du nom (globale). */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse create(CategoryDepensePlateformeRequest request) {
        ensureNomAvailable(request.nom());
        return new CategoryDepensePlateformeResponse(domainService.create(request));
    }

    @Override
    public CategoryDepensePlateforme findById(UUID id) {
        return domainService.findById(id);
    }

    @Override
    public CategoryDepensePlateformeResponse findResponseById(UUID id) {
        return new CategoryDepensePlateformeResponse(domainService.findById(id));
    }

    @Override
    public Page<CategoryDepensePlateformeResponse> findAll(CategoryDepensePlateformeFilter filter) {
        return domainService.findResponses(filter);
    }

    /** Met à jour la catégorie après contrôle d'unicité du nom (si changé). */
    @Override
    @Transactional
    public CategoryDepensePlateformeResponse update(UUID id, CategoryDepensePlateformeRequest request) {
        CategoryDepensePlateforme category = domainService.findById(id);
        if (!category.getNom().equals(request.nom())) {
            ensureNomAvailable(request.nom());
        }
        category.setNom(request.nom());
        category.setDescription(request.description());
        if (request.actif() != null) {
            category.setActif(request.actif());
        }
        return new CategoryDepensePlateformeResponse(domainService.save(category));
    }

    @Override
    @Transactional
    public void delete(UUID id) {
        domainService.delete(domainService.findById(id));
    }

    /** Lève UniqueResourceException si une catégorie portant ce nom existe déjà. */
    private void ensureNomAvailable(String nom) {
        if (domainService.existsByNom(nom)) {
            throw new UniqueResourceException("categoryDepensePlateforme.nom.alreadyExists", nom);
        }
    }
}
