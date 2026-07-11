package org.store.produit.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.produit.application.dto.ProductSearchResponse;

import org.store.security.application.dto.UserPrincipal;

import java.util.UUID;

public interface IProductSearchService {

    /**
     * Recherche produits disponibles (avec au moins un lot actif) dans un magasin, par nom ou référence (partial, insensible à la casse).
     * Le magasinId est obligatoire pour un OWNER et implicite (UserPrincipal.magasinId) pour un EMPLOYE.
     * Retourne 1 ligne par produit avec sous-liste des PF actifs (quantité + prix de vente courant).
     */
    Page<ProductSearchResponse> search(String searchTerm, UUID magasinId, Pageable pageable);

    /**
     * Recherche produits de l'entreprise SANS filtre de stock, pour les contextes d'ajout de stock (achat, entrée initiale).
     * Le magasinId sert uniquement au contrôle d'accès (obligatoire pour un OWNER, implicite pour un EMPLOYE) : la liste de produits reste entreprise-wide.
     * Retourne 1 ligne par produit avec sous-liste des PF existants (sans quantité en stock).
     */
    Page<ProductSearchResponse> searchAll(String searchTerm, UUID magasinId, Pageable pageable);

    /** Résout le magasinId effectif : paramètre fourni, ou magasin de l'employé connecté. */
    UUID resolveSearchMagasinId(UserPrincipal currentUser, UUID requestedMagasinId);
}
