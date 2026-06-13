package org.store.produit.application.service;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.domain.model.ProductFournisseur;

import java.util.UUID;

public interface IProductFournisseurService {

    /**
     * Création du lien produit ↔ fournisseur ↔ qualité avec prix d'achat et prix de vente pour l'entreprise du caller.
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
     * Modification du prix d'achat, prix de vente, référence fournisseur et origine d'un lien existant. Les FK product/fournisseur/quality sont immuables.
     */
    ProductFournisseurResponse update(UUID id, ProductFournisseurRequest productFournisseurRequest);

    /**
     * Met à jour uniquement le prix de vente. Utilisé après chaque achat (snapshot ligne) et via PUT manager pour ajustement libre.
     */
    ProductFournisseurResponse updatePrixVente(UUID id, BigDecimal prixVente);

    /**
     * Suppression d'un lien produit-fournisseur de l'entreprise du caller.
     */
    void delete(UUID id);

    /**
     * Vérifie qu'un lien appartient à l'entreprise du caller (via product.entreprise). Throw `ForbiddenException("productFournisseur.notOwned")` sinon.
     */
    ProductFournisseur ensureBelongsToCurrentEntreprise(ProductFournisseur productFournisseur);

    /**
     * Vérifie qu'aucun lien (product, fournisseur, quality) n'existe déjà. Throw `UniqueResourceException("productFournisseur.alreadyExists")` sinon.
     */
    void ensureTripletAvailable(UUID productId, UUID fournisseurId, UUID qualityId);

    /**
     * Met à jour le prix de vente courant du PF avec celui de l'entité fournie (réutilisé par AchatServiceImpl après chaque ligne d'achat).
     */
    ProductFournisseur applyPrixVenteFromPurchase(ProductFournisseur productFournisseur, BigDecimal newPrixVente);

    /**
     * Retourne le lien existant (productId, fournisseurId, qualityId). Lève EntityException s'il n'existe pas.
     * Utilisé pour les ventes où le PF doit obligatoirement déjà exister dans le catalogue.
     */
    ProductFournisseur findByTriplet(UUID productId, UUID fournisseurId, UUID qualityId);

    /**
     * Retourne le lien existant (productId, fournisseurId, qualityId) ou le crée s'il n'existe pas encore.
     * Utilisé par le formulaire d'achat pour résoudre le PF en une seule transaction serveur.
     */
    ProductFournisseurResponse findOrCreate(ProductFournisseurRequest request);

    /**
     * Vérifie la règle métier prixVente > prixAchat (marge strictement positive). Throw `BadArgumentException("productFournisseur.prixVente.belowOrEqualAchat")` sinon.
     */
    void ensurePrixVenteGreaterThanPrixAchat(BigDecimal prixVente, BigDecimal prixAchat);
}
