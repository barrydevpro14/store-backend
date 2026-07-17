package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.produit.application.dto.ImageMetadataResponse;
import org.store.produit.application.dto.ProductFilter;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.domain.model.Product;

import java.util.List;
import java.util.Optional;
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
     * Listing paginé + filtré des produits de l'entreprise du caller.
     */
    Page<ProductResponse> findAll(ProductFilter filter);

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
     * Vérifie qu'aucun produit de l'entreprise donnée ne porte déjà ce couple (reference, nom). Throw `UniqueResourceException("product.referenceNom.alreadyExists")` sinon.
     */
    void ensureReferenceAndNomAvailable(String reference, String nom, UUID entrepriseId);

    /**
     * Retourne true si un produit avec ce couple (reference, nom) existe déjà
     * pour l'entreprise du caller. Utilisé par l'import pour la détection des doublons.
     */
    boolean existsByReferenceAndNom(String reference, String nom);

    /**
     * Recherche un produit par son couple (reference, nom) pour l'entreprise du caller.
     */
    Optional<Product> findByReferenceAndNom(String reference, String nom);

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

    /**
     * Retourne le binaire de l'image principale d'un produit de l'entreprise du caller, avec le content-type détecté.
     */
    ImageDownloadResponse getImagePrincipal(UUID id);

    /**
     * Retourne le binaire d'une image de la galerie d'un produit de l'entreprise du caller, avec le content-type détecté.
     */
    ImageDownloadResponse getImage(UUID productId, UUID imageId);

    /**
     * Supprime une image de la galerie d'un produit de l'entreprise du caller.
     */
    void deleteImage(UUID productId, UUID imageId);

    /**
     * Retourne les métadonnées (id, date, contentType, url) de toutes les images de la galerie d'un produit de l'entreprise du caller.
     */
    List<ImageMetadataResponse> listImages(UUID productId);
}
