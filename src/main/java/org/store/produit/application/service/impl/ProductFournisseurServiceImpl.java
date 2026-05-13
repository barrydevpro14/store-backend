package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.service.ProductFournisseurDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère le CRUD des liens produit ↔ fournisseur (prix d'achat, référence fournisseur, origine), scopé par entreprise.
 */
@Service
@Transactional(readOnly = true)
public class ProductFournisseurServiceImpl implements IProductFournisseurService {

    private final ProductFournisseurDomainService productFournisseurDomainService;
    private final IProductService productService;
    private final IFournisseurService fournisseurService;
    private final ICurrentUserService currentUserService;

    public ProductFournisseurServiceImpl(ProductFournisseurDomainService productFournisseurDomainService,
                                         IProductService productService,
                                         IFournisseurService fournisseurService,
                                         ICurrentUserService currentUserService) {
        this.productFournisseurDomainService = productFournisseurDomainService;
        this.productService = productService;
        this.fournisseurService = fournisseurService;
        this.currentUserService = currentUserService;
    }

    /** Crée le lien après vérification que product et fournisseur appartiennent à l'entreprise du caller et qu'aucun lien n'existe déjà. */
    @Override
    @Transactional
    public ProductFournisseurResponse create(ProductFournisseurRequest productFournisseurRequest) {
        Product product = productService.ensureBelongsToCurrentEntreprise(productService.findById(productFournisseurRequest.productId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(productFournisseurRequest.fournisseurId()));
        ensurePairAvailable(product.getId(), fournisseur.getId());
        return new ProductFournisseurResponse(productFournisseurDomainService.create(productFournisseurRequest, product, fournisseur));
    }

    /** Retourne le lien ou lève `EntityException`. */
    @Override
    public ProductFournisseur findById(UUID id) {
        return productFournisseurDomainService.findById(id);
    }

    /** Retourne le lien en `Response` après vérification de l'appartenance à l'entreprise du caller. */
    @Override
    public ProductFournisseurResponse findResponseById(UUID id) {
        ProductFournisseur productFournisseur = ensureBelongsToCurrentEntreprise(productFournisseurDomainService.findById(id));
        return new ProductFournisseurResponse(productFournisseur);
    }

    /** Liste paginée des liens de l'entreprise du caller (tous produits, tous fournisseurs). */
    @Override
    public Page<ProductFournisseurResponse> findAllByCurrentEntreprise(Pageable pageable) {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        return productFournisseurDomainService.findResponsesByEntrepriseId(entrepriseId, pageable);
    }

    /** Liste paginée des fournisseurs d'un produit (filtre métier pratique pour le frontend). */
    @Override
    public Page<ProductFournisseurResponse> findAllByProductId(UUID productId, Pageable pageable) {
        productService.ensureBelongsToCurrentEntreprise(productService.findById(productId));
        return productFournisseurDomainService.findResponsesByProductId(productId, pageable);
    }

    /** Met à jour prix d'achat, référence fournisseur et origine ; product et fournisseur restent immuables. */
    @Override
    @Transactional
    public ProductFournisseurResponse update(UUID id, ProductFournisseurRequest productFournisseurRequest) {
        ProductFournisseur productFournisseur = ensureBelongsToCurrentEntreprise(productFournisseurDomainService.findById(id));
        productFournisseur.setPrixAchat(productFournisseurRequest.prixAchat());
        productFournisseur.setReferenceFournisseur(productFournisseurRequest.referenceFournisseur());
        productFournisseur.setOrigine(productFournisseurRequest.origine());
        return new ProductFournisseurResponse(productFournisseurDomainService.save(productFournisseur));
    }

    /** Supprime le lien après contrôle d'appartenance à l'entreprise du caller. */
    @Override
    @Transactional
    public void delete(UUID id) {
        ProductFournisseur productFournisseur = ensureBelongsToCurrentEntreprise(productFournisseurDomainService.findById(id));
        productFournisseurDomainService.delete(productFournisseur);
    }

    /** Lève `ForbiddenException` si le lien n'appartient pas à l'entreprise du caller (vérification via product.entreprise). */
    @Override
    public ProductFournisseur ensureBelongsToCurrentEntreprise(ProductFournisseur productFournisseur) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!productFournisseur.getProduct().getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("productFournisseur.notOwned");
        }
        return productFournisseur;
    }

    /** Lève `UniqueResourceException` si un lien (product, fournisseur) existe déjà. */
    @Override
    public void ensurePairAvailable(UUID productId, UUID fournisseurId) {
        if (productFournisseurDomainService.existsByProductIdAndFournisseurId(productId, fournisseurId)) {
            throw new UniqueResourceException("productFournisseur.alreadyExists");
        }
    }
}
