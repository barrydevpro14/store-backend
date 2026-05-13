package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.UUID;

public interface IProductFournisseurService {

    /**
     * Création du lien produit ↔ fournisseur avec prix d'achat pour l'entreprise du caller.
     */
    ProductFournisseurResponse create(ProductFournisseurRequest productFournisseurRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    ProductFournisseur findById(UUID id);

    /**
     * Lecture par id, scopée sur l'entreprise du caller.
     */
    ProductFournisseurResponse findResponseById(UUID id);

    /**
     * Listing paginé des liens produit-fournisseur de l'entreprise du caller.
     */
    Page<ProductFournisseurResponse> findAllByCurrentEntreprise(Pageable pageable);

    /**
     * Listing paginé des fournisseurs et prix d'achat d'un produit donné de l'entreprise du caller.
     */
    Page<ProductFournisseurResponse> findAllByProductId(UUID productId, Pageable pageable);

    /**
     * Modification du prix d'achat, référence fournisseur et origine d'un lien existant. Les FK product/fournisseur sont immuables.
     */
    ProductFournisseurResponse update(UUID id, ProductFournisseurRequest productFournisseurRequest);

    /**
     * Suppression d'un lien produit-fournisseur de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'un lien appartient à l'entreprise du caller (via product.entreprise). Throw `ForbiddenException("productFournisseur.notOwned")` sinon.
     */
    ProductFournisseur ensureBelongsToCurrentEntreprise(ProductFournisseur productFournisseur);

    /**
     * Vérifie qu'aucun lien (product, fournisseur) n'existe déjà. Throw `UniqueResourceException("productFournisseur.alreadyExists")` sinon.
     */
    void ensurePairAvailable(UUID productId, UUID fournisseurId);
}
