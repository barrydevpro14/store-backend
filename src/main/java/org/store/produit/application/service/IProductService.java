package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.web.multipart.MultipartFile;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.domain.model.Product;

import java.util.List;
import java.util.UUID;

public interface IProductService {

    /**
     * Création d'un produit pour l'entreprise du caller.
     */
    ProductResponse create(ProductRequest productRequest);

    /**
     * Lecture interne par id (utilisée par d'autres agrégats).
     */
    Product findById(UUID id);

    /**
     * Lecture par id, scopée sur l'entreprise du caller.
     */
    ProductResponse findResponseById(UUID id);

    /**
     * Listing paginé des produits de l'entreprise du caller.
     */
    Page<ProductResponse> findAllByCurrentEntreprise(Pageable pageable);

    /**
     * Modification d'un produit de l'entreprise du caller.
     */
    ProductResponse update(UUID id, ProductRequest productRequest);

    /**
     * Suppression d'un produit de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'un produit appartient à l'entreprise du caller. Throw `ForbiddenException("product.notOwned")` sinon.
     */
    Product ensureBelongsToCurrentEntreprise(Product product);

    /**
     * Vérifie qu'aucun produit de l'entreprise donnée ne porte déjà cette référence. Throw `UniqueResourceException("product.reference.alreadyExists")` sinon.
     */
    void ensureReferenceAvailable(String reference, UUID entrepriseId);

    /**
     * Téléverse une image principale (remplace l'existante) pour un produit de l'entreprise du caller.
     */
    ProductResponse uploadImagePrincipal(UUID id, MultipartFile file);

    /**
     * Supprime l'image principale d'un produit de l'entreprise du caller (idempotent).
     */
    void deleteImagePrincipal(UUID id);

    /**
     * Téléverse plusieurs images dans la galerie d'un produit de l'entreprise du caller. Retourne les ids des images créées.
     */
    List<UUID> uploadImages(UUID id, List<MultipartFile> files);
}
