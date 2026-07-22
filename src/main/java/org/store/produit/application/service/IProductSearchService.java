package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.produit.application.dto.ProductSelectorResponse;
import org.store.produit.application.dto.ProductVariantSearchResponse;

import org.store.security.application.dto.UserPrincipal;

import java.util.UUID;

public interface IProductSearchService {

    /**
     * Recherche variantes (ProductFournisseur) avec stock actif dans le magasin, par nom/référence/catégorie.
     * Retourne 1 ligne par variante avec label pré-construit et quantiteEnStock agrégée.
     */
    Page<ProductVariantSearchResponse> search(String searchTerm, UUID magasinId, Pageable pageable);

    /**
     * Recherche produits de l'entreprise SANS filtre de stock, pour les contextes d'ajout de stock (achat, entrée initiale).
     * Le magasinId sert uniquement au contrôle d'accès (obligatoire pour un OWNER, implicite pour un EMPLOYE) : la liste de produits reste entreprise-wide.
     * Retourne 1 ligne par produit : id, nom, référence, catégorie — sans info de stock ni fournisseur.
     */
    Page<ProductSelectorResponse> searchAll(String searchTerm, UUID magasinId, Pageable pageable);

    /** Résout le magasinId effectif : paramètre fourni, ou magasin de l'employé connecté. */
    UUID resolveSearchMagasinId(UserPrincipal currentUser, UUID requestedMagasinId);
}
