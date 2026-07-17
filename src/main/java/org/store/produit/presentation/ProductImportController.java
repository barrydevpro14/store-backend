package org.store.produit.presentation;

import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.produit.application.dto.ProductImportRequest;
import org.store.produit.application.dto.ProductImportResult;
import org.store.produit.application.service.IProductImportService;

@RestController
@RequestMapping(ProductImportController.BASE_PATH)
public class ProductImportController {

    public static final String BASE_PATH = "/api/v1/products/import";

    private final IProductImportService productImportService;

    public ProductImportController(IProductImportService productImportService) {
        this.productImportService = productImportService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_IMPORT')")
    public ResponseEntity<ProductImportResult> importProducts(@Valid @RequestBody ProductImportRequest request) {
        return ResponseEntity.ok(productImportService.importProducts(request));
    }
}
