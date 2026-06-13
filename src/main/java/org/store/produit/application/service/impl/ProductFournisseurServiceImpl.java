package org.store.produit.application.service.impl;

import java.math.BigDecimal;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.IFournisseurService;
import org.store.achat.domain.model.Fournisseur;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.tools.OwnershipHelper;
import org.store.produit.application.dto.ProductFournisseurCreate;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.service.IProductFournisseurService;
import org.store.produit.application.service.IProductService;
import org.store.produit.application.service.IQualityService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.service.ProductFournisseurDomainService;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Gère le CRUD des liens produit ↔ fournisseur ↔ qualité (prix d'achat, prix de vente courant, traçabilité), scopé par entreprise.
 */
@Service
@Transactional(readOnly = true)
public class ProductFournisseurServiceImpl implements IProductFournisseurService {

    private final ProductFournisseurDomainService productFournisseurDomainService;
    private final IProductService productService;
    private final IFournisseurService fournisseurService;
    private final IQualityService qualityService;
    private final ICurrentUserService currentUserService;

    public ProductFournisseurServiceImpl(ProductFournisseurDomainService productFournisseurDomainService,
                                         IProductService productService,
                                         IFournisseurService fournisseurService,
                                         IQualityService qualityService,
                                         ICurrentUserService currentUserService) {
        this.productFournisseurDomainService = productFournisseurDomainService;
        this.productService = productService;
        this.fournisseurService = fournisseurService;
        this.qualityService = qualityService;
        this.currentUserService = currentUserService;
    }

    /** Crée le lien après vérification des appartenances (product/fournisseur/quality) à l'entreprise et de l'unicité du triplet ; valide prixVente > prixAchat. */
    @Override
    @Transactional
    public ProductFournisseurResponse create(ProductFournisseurRequest productFournisseurRequest) {
        Product product = productService.ensureBelongsToCurrentEntreprise(productService.findById(productFournisseurRequest.productId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(productFournisseurRequest.fournisseurId()));
        Quality quality = qualityService.ensureBelongsToCurrentEntreprise(qualityService.findById(productFournisseurRequest.qualityId()));

        ensurePrixVenteGreaterThanPrixAchat(productFournisseurRequest.prixVente(), productFournisseurRequest.prixAchat());
        ensureTripletAvailable(product.getId(), fournisseur.getId(), quality.getId());

        return new ProductFournisseurResponse(productFournisseurDomainService.create(
                new ProductFournisseurCreate(productFournisseurRequest, product, fournisseur, quality)));
    }

    /** Retourne le lien existant (productId, fournisseurId, qualityId), lève EntityException s'il n'existe pas. */
    @Override
    public ProductFournisseur findByTriplet(UUID productId, UUID fournisseurId, UUID qualityId) {
        return productFournisseurDomainService.findByTriplet(productId, fournisseurId, qualityId)
                .orElseThrow(() -> new org.store.common.exceptions.EntityException("productFournisseur.notFound", productId));
    }

    /**
     * Retourne le lien existant (productId, fournisseurId, qualityId) ou le crée.
     * En cas de violation de contrainte unique (race condition), relit l'enregistrement
     * déjà persisté plutôt que de propager une erreur.
     */
    @Override
    @Transactional
    public ProductFournisseurResponse findOrCreate(ProductFournisseurRequest request) {
        Product product = productService.ensureBelongsToCurrentEntreprise(productService.findById(request.productId()));
        Fournisseur fournisseur = fournisseurService.ensureBelongsToCurrentEntreprise(fournisseurService.findById(request.fournisseurId()));
        Quality quality = qualityService.ensureBelongsToCurrentEntreprise(qualityService.findById(request.qualityId()));

        java.util.Optional<ProductFournisseur> existing =
                productFournisseurDomainService.findByTriplet(product.getId(), fournisseur.getId(), quality.getId());

        if (existing.isPresent()) {
            return new ProductFournisseurResponse(existing.get());
        }

        ensurePrixVenteGreaterThanPrixAchat(request.prixVente(), request.prixAchat());

        try {
            return new ProductFournisseurResponse(productFournisseurDomainService.create(
                    new ProductFournisseurCreate(request, product, fournisseur, quality)));
        } catch (org.springframework.dao.DataIntegrityViolationException e) {
            return productFournisseurDomainService
                    .findByTriplet(product.getId(), fournisseur.getId(), quality.getId())
                    .map(ProductFournisseurResponse::new)
                    .orElseThrow(() -> e);
        }
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

    /** Met à jour prix d'achat, prix de vente, référence fournisseur et origine ; product/fournisseur/quality restent immuables ; valide prixVente > prixAchat. */
    @Override
    @Transactional
    public ProductFournisseurResponse update(UUID id, ProductFournisseurRequest productFournisseurRequest) {
        ProductFournisseur productFournisseur = ensureBelongsToCurrentEntreprise(productFournisseurDomainService.findById(id));
        ensurePrixVenteGreaterThanPrixAchat(productFournisseurRequest.prixVente(), productFournisseurRequest.prixAchat());

        productFournisseur.setPrixAchat(productFournisseurRequest.prixAchat());
        productFournisseur.setPrixVente(productFournisseurRequest.prixVente());
        productFournisseur.setReferenceFournisseur(productFournisseurRequest.referenceFournisseur());
        productFournisseur.setOrigine(productFournisseurRequest.origine());
        return new ProductFournisseurResponse(productFournisseurDomainService.save(productFournisseur));
    }

    /** Met à jour uniquement le prix de vente courant du PF (manager) sans contrainte sur l'ancien prix, mais avec contrainte prixVente > prixAchat actuel. */
    @Override
    @Transactional
    public ProductFournisseurResponse updatePrixVente(UUID id, BigDecimal prixVente) {
        ProductFournisseur productFournisseur = ensureBelongsToCurrentEntreprise(productFournisseurDomainService.findById(id));
        ensurePrixVenteGreaterThanPrixAchat(prixVente, productFournisseur.getPrixAchat());
        return new ProductFournisseurResponse(productFournisseurDomainService.updatePrixVente(productFournisseur, prixVente));
    }

    /** Met à jour le prix de vente sans revérifier l'appartenance entreprise (appelé depuis le flux d'achat déjà scopé). */
    @Override
    @Transactional
    public ProductFournisseur applyPrixVenteFromPurchase(ProductFournisseur productFournisseur, BigDecimal newPrixVente) {
        return productFournisseurDomainService.updatePrixVente(productFournisseur, newPrixVente);
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
        return OwnershipHelper.ensureOwnership(
                productFournisseur,
                productFournisseur.getProduct().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "productFournisseur.notOwned"
        );
    }

    /** Lève `UniqueResourceException` si un lien (product, fournisseur, quality) existe déjà. */
    @Override
    public void ensureTripletAvailable(UUID productId, UUID fournisseurId, UUID qualityId) {
        if (productFournisseurDomainService.existsByProductIdAndFournisseurIdAndQualityId(productId, fournisseurId, qualityId)) {
            throw new UniqueResourceException("productFournisseur.alreadyExists");
        }
    }

    /** Vérifie que prixVente > prixAchat (marge strictement positive). */
    @Override
    public void ensurePrixVenteGreaterThanPrixAchat(BigDecimal prixVente, BigDecimal prixAchat) {
        if (prixVente.compareTo(prixAchat) <= 0) {
            throw new BadArgumentException("productFournisseur.prixVente.belowOrEqualAchat");
        }
    }
}
