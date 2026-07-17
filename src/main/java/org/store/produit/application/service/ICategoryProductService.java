package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.store.produit.application.dto.CategoryProductFilter;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.domain.model.CategoryProduct;

import java.util.UUID;

public interface ICategoryProductService {

    /**
     * Création d'une catégorie de produit pour l'entreprise du caller.
     */
    CategoryProductResponse create(CategoryProductRequest categoryProductRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    CategoryProduct findById(UUID id);

    /**
     * Lecture par id, scopée sur l'entreprise du caller.
     */
    CategoryProductResponse findResponseById(UUID id);

    /**
     * Listing paginé + filtré des catégories de l'entreprise du caller.
     */
    Page<CategoryProductResponse> findAll(CategoryProductFilter filter);

    /**
     * Modification d'une catégorie de l'entreprise du caller.
     */
    CategoryProductResponse update(UUID id, CategoryProductRequest categoryProductRequest);

    /**
     * Suppression d'une catégorie de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'une catégorie appartient à l'entreprise du caller. Throw `ForbiddenException("categoryProduct.notOwned")` sinon.
     */
    CategoryProduct ensureBelongsToCurrentEntreprise(CategoryProduct categoryProduct);

    /**
     * Vérifie qu'aucune catégorie de l'entreprise donnée ne porte déjà ce libellé. Throw `UniqueResourceException("categoryProduct.libelle.alreadyExists")` sinon.
     */
    void ensureLibelleAvailable(String libelle, UUID entrepriseId);

    /**
     * Retourne true si une catégorie portant ce libellé existe déjà pour l'entreprise du caller.
     */
    boolean existsByLibelle(String libelle);

    /**
     * Retourne la catégorie dont le libellé correspond (insensible à la casse) pour l'entreprise
     * du caller, ou en crée une automatiquement si elle n'existe pas encore.
     * Utilisé par l'import produit pour résoudre les catégories à la volée.
     */
    CategoryProduct findOrCreateByLibelle(String libelle);
}
