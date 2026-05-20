package org.store.produit.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.application.service.IMagasinService;
import org.store.produit.application.dto.ProductFournisseurStockResponse;
import org.store.produit.application.dto.ProductSearchResponse;
import org.store.produit.application.service.IProductSearchService;
import org.store.produit.domain.model.Product;
import org.store.produit.domain.model.ProductFournisseur;
import org.store.produit.domain.service.ProductDomainService;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.stock.application.service.IEntreeStockService;
import org.store.stock.domain.model.EntreeStock;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Recherche produits disponibles en stock pour un magasin, avec sous-liste des ProductFournisseur actifs et quantités agrégées.
 */
@Service
@Transactional(readOnly = true)
public class ProductSearchServiceImpl implements IProductSearchService {

    private final ProductDomainService productDomainService;
    private final IMagasinService magasinService;
    private final IEntreeStockService entreeStockService;
    private final ICurrentUserService currentUserService;

    public ProductSearchServiceImpl(ProductDomainService productDomainService,
                                    IMagasinService magasinService,
                                    IEntreeStockService entreeStockService,
                                    ICurrentUserService currentUserService) {
        this.productDomainService = productDomainService;
        this.magasinService = magasinService;
        this.entreeStockService = entreeStockService;
        this.currentUserService = currentUserService;
    }

    /** Recherche produits avec lots actifs dans le magasin scopé, agrège la quantité par produit et expose la sous-liste des PF actifs. */
    @Override
    public Page<ProductSearchResponse> search(String searchTerm, UUID magasinId, Pageable pageable) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        UUID effectiveMagasinId = resolveSearchMagasinId(currentUser, magasinId);
        magasinService.ensureAccessibleByCurrentUser(magasinService.findById(effectiveMagasinId));

        String normalizedSearchTerm = (searchTerm == null || searchTerm.isBlank()) ? null : searchTerm.trim();
        Page<Product> productsPage = productDomainService.searchByEntrepriseWithActiveLots(
                normalizedSearchTerm, effectiveMagasinId, currentUser.entrepriseId(), pageable);

        if (productsPage.isEmpty()) {
            return Page.empty(pageable);
        }

        List<UUID> productIds = productsPage.stream().map(Product::getId).toList();
        List<EntreeStock> activeLots = entreeStockService.findActiveLotsByMagasinAndProductIds(effectiveMagasinId, productIds);

        Map<UUID, List<EntreeStock>> lotsByProductId = activeLots.stream()
                .collect(Collectors.groupingBy(lot -> lot.getProduit().getId()));

        return productsPage.map(product -> buildSearchResponse(product, lotsByProductId.getOrDefault(product.getId(), List.of())));
    }

    /** Résout le magasinId pour la recherche : pour un EMPLOYE absence de paramètre = son magasin ; pour un OWNER le paramètre est obligatoire. */
    private UUID resolveSearchMagasinId(UserPrincipal currentUser, UUID requestedMagasinId) {
        if (requestedMagasinId != null) {
            return requestedMagasinId;
        }
        if (currentUser.magasinId() == null) {
            throw new BadArgumentException("product.search.magasinIdRequired");
        }
        return currentUser.magasinId();
    }

    /** Construit la `ProductSearchResponse` pour un produit en agrégeant ses lots actifs par ProductFournisseur. */
    private ProductSearchResponse buildSearchResponse(Product product, List<EntreeStock> productLots) {
        Map<UUID, List<EntreeStock>> lotsByPfId = productLots.stream()
                .collect(Collectors.groupingBy(lot -> lot.getProductFournisseur().getId()));

        List<ProductFournisseurStockResponse> pfStockList = lotsByPfId.values().stream()
                .map(this::buildPfStock)
                .toList();

        int totalQuantite = productLots.stream().mapToInt(EntreeStock::getQuantiteRestante).sum();

        return new ProductSearchResponse(product, totalQuantite, pfStockList);
    }

    /** Agrège les lots d'un même ProductFournisseur en un sous-DTO `ProductFournisseurStockResponse`. */
    private ProductFournisseurStockResponse buildPfStock(List<EntreeStock> lotsOfPf) {
        ProductFournisseur productFournisseur = lotsOfPf.get(0).getProductFournisseur();
        int sumQuantite = lotsOfPf.stream().mapToInt(EntreeStock::getQuantiteRestante).sum();
        return new ProductFournisseurStockResponse(productFournisseur, sumQuantite);
    }
}
