package org.store.produit.application.service.impl;

import org.springframework.stereotype.Service;
import org.store.produit.application.dto.ProductImportError;
import org.store.produit.application.dto.ProductImportItem;
import org.store.produit.application.dto.ProductImportRequest;
import org.store.produit.application.dto.ProductImportResult;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.service.ICategoryProductService;
import org.store.produit.application.service.IProductImportService;
import org.store.produit.application.service.IProductService;
import org.store.produit.domain.model.CategoryProduct;

import java.util.ArrayList;
import java.util.List;

/**
 * Import de produits dans l'ERP depuis le catalogue Bhantic ou un fichier Excel/CSV.
 * Le service est agnostique à la source : il reçoit toujours une {@link ProductImportRequest}.
 * Chaque produit est créé dans sa propre transaction (via {@link IProductService#create}) —
 * un échec sur un item n'annule pas les créations précédentes.
 */
@Service
public class ProductImportServiceImpl implements IProductImportService {

    private final IProductService productService;
    private final ICategoryProductService categoryProductService;

    public ProductImportServiceImpl(IProductService productService,
                                    ICategoryProductService categoryProductService) {
        this.productService = productService;
        this.categoryProductService = categoryProductService;
    }

    @Override
    public ProductImportResult importProducts(ProductImportRequest request) {
        int imported = 0;
        int ignored = 0;
        int categoriesCreated = 0;
        List<ProductImportError> errors = new ArrayList<>();

        for (ProductImportItem item : request.produits()) {
            try {
                if (productService.existsByReferenceAndNom(item.reference(), item.libelle())) {
                    ignored++;
                    continue;
                }

                if (item.categorie() == null || item.categorie().isBlank()) {
                    errors.add(new ProductImportError(item.reference(), item.libelle(), "categorie obligatoire"));
                    continue;
                }

                boolean categoryExisted = categoryProductService.existsByLibelle(item.categorie());
                CategoryProduct category = categoryProductService.findOrCreateByLibelle(item.categorie());
                categoriesCreated += categoryExisted ? 0 : 1;

                productService.create(new ProductRequest(item.libelle(), item.reference(), item.description(), category.getId()));
                imported++;

            } catch (Exception e) {
                errors.add(new ProductImportError(item.reference(), item.libelle(), e.getMessage()));
            }
        }

        return new ProductImportResult(imported, ignored, categoriesCreated, errors);
    }
}
