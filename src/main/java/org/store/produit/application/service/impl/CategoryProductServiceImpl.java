package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.tools.OwnershipHelper;
import org.store.entreprise.application.service.IEntrepriseService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.CategoryProductFilter;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.service.ICategoryProductService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.service.CategoryProductDomainService;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère le CRUD des catégories de produit, scopé sur l'entreprise de l'utilisateur courant.
 */
@Service
public class CategoryProductServiceImpl implements ICategoryProductService {

    private final CategoryProductDomainService categoryProductDomainService;
    private final IEntrepriseService entrepriseService;
    private final ICurrentUserService currentUserService;

    public CategoryProductServiceImpl(CategoryProductDomainService categoryProductDomainService,
                                      IEntrepriseService entrepriseService,
                                      ICurrentUserService currentUserService) {
        this.categoryProductDomainService = categoryProductDomainService;
        this.entrepriseService = entrepriseService;
        this.currentUserService = currentUserService;
    }

    /** Crée une catégorie pour l'entreprise du caller après contrôle d'unicité du libellé. */
    @Override
    @Transactional
    public CategoryProductResponse create(CategoryProductRequest categoryProductRequest) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        ensureLibelleAvailable(categoryProductRequest.libelle(), entrepriseId);
        Entreprise entreprise = entrepriseService.findById(entrepriseId);
        return new CategoryProductResponse(categoryProductDomainService.create(categoryProductRequest, entreprise));
    }

    /** Retourne la catégorie ou lève `EntityException`. */
    @Override
    public CategoryProduct findById(UUID id) {
        return categoryProductDomainService.findById(id);
    }

    /** Retourne la catégorie en `Response` après vérification de l'appartenance à l'entreprise du caller. */
    @Override
    public CategoryProductResponse findResponseById(UUID id) {
        CategoryProduct categoryProduct = ensureBelongsToCurrentEntreprise(categoryProductDomainService.findById(id));
        return new CategoryProductResponse(categoryProduct);
    }

    /** Liste paginée + filtrée des catégories de l'entreprise du caller. */
    @Override
    public Page<CategoryProductResponse> findAll(CategoryProductFilter filter) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return categoryProductDomainService.findResponsesByFilter(filter, entrepriseId);
    }

    /** Met à jour libellé et description après contrôle d'appartenance et d'unicité. */
    @Override
    @Transactional
    public CategoryProductResponse update(UUID id, CategoryProductRequest categoryProductRequest) {
        CategoryProduct categoryProduct = ensureBelongsToCurrentEntreprise(categoryProductDomainService.findById(id));
        if (!categoryProduct.getLibelle().equals(categoryProductRequest.libelle())) {
            ensureLibelleAvailable(categoryProductRequest.libelle(), categoryProduct.getEntreprise().getId());
        }
        categoryProduct.setLibelle(categoryProductRequest.libelle());
        categoryProduct.setDescription(categoryProductRequest.description());
        return new CategoryProductResponse(categoryProductDomainService.save(categoryProduct));
    }

    /** Supprime la catégorie après contrôle d'appartenance à l'entreprise du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        CategoryProduct categoryProduct = ensureBelongsToCurrentEntreprise(categoryProductDomainService.findById(id));
        categoryProductDomainService.delete(categoryProduct);
    }

    /** Lève `ForbiddenException` si la catégorie n'appartient pas à l'entreprise du caller. */
    @Override
    public CategoryProduct ensureBelongsToCurrentEntreprise(CategoryProduct categoryProduct) {
        return OwnershipHelper.ensureOwnership(
                categoryProduct,
                categoryProduct.getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "categoryProduct.notOwned"
        );
    }

    /** Lève `UniqueResourceException` si le libellé est déjà utilisé dans l'entreprise. */
    @Override
    public void ensureLibelleAvailable(String libelle, UUID entrepriseId) {
        if (categoryProductDomainService.existsByLibelleAndEntrepriseId(libelle.toUpperCase(), entrepriseId)) {
            throw new UniqueResourceException("categoryProduct.libelle.alreadyExists", libelle);
        }
    }
}
