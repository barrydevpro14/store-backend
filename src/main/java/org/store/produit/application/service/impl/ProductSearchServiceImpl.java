package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.application.service.IMagasinService;
import org.store.produit.application.dto.ProductSelectorResponse;
import org.store.produit.application.dto.ProductVariantSearchResponse;
import org.store.produit.application.service.IProductSearchService;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

/**
 * Recherche produits pour le sélecteur de vente (variantes avec stock) et le sélecteur d'achat/stock (tous produits).
 */
@Service
@Transactional(readOnly = true)
public class ProductSearchServiceImpl implements IProductSearchService {

    private final ProductDomainService productDomainService;
    private final IMagasinService magasinService;
    private final ICurrentUserService currentUserService;

    public ProductSearchServiceImpl(ProductDomainService productDomainService,
                                    IMagasinService magasinService,
                                    ICurrentUserService currentUserService) {
        this.productDomainService = productDomainService;
        this.magasinService = magasinService;
        this.currentUserService = currentUserService;
    }

    /** Recherche variantes avec stock actif dans le magasin — label et quantiteEnStock agrégés en JPQL. */
    @Override
    public Page<ProductVariantSearchResponse> search(String searchTerm, UUID magasinId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID effectiveMagasinId = resolveSearchMagasinId(currentUser, magasinId);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(effectiveMagasinId));

        String normalizedSearchTerm = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();

        return productDomainService.searchVariants(normalizedSearchTerm, effectiveMagasinId, currentUser.entrepriseId(), pageable);
    }

    /** Recherche produits de l'entreprise sans filtre de stock : vérifie l'accès au magasin puis retourne les produits (id, nom, référence, catégorie). */
    @Override
    public Page<ProductSelectorResponse> searchAll(String searchTerm, UUID magasinId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID effectiveMagasinId = resolveSearchMagasinId(currentUser, magasinId);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(effectiveMagasinId));

        String normalizedSearchTerm = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();

        return productDomainService.searchResponsesByEntreprise(normalizedSearchTerm, currentUser.entrepriseId(), pageable);
    }

    /** Résout le magasinId pour la recherche : pour un EMPLOYE absence de paramètre = son magasin ; pour un OWNER le paramètre est obligatoire. */
    @Override
    public UUID resolveSearchMagasinId(UserPrincipal currentUser, UUID requestedMagasinId) {
        if (requestedMagasinId != null) {
            return requestedMagasinId;
        }
        if (currentUser.magasinId() == null) {
            throw new BadArgumentException("product.search.magasinIdRequired");
        }
        return currentUser.magasinId();
    }
}
