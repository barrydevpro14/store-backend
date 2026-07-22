package org.store.produit.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.produit.application.dto.ProductFournisseurPrixVenteRequest;
import org.store.produit.application.dto.ProductFournisseurRequest;
import org.store.produit.application.dto.ProductFournisseurResponse;
import org.store.produit.application.service.IProductFournisseurService;

import java.util.UUID;

@RestController
@RequestMapping(ProductFournisseurController.BASE_PATH)
public class ProductFournisseurController {

    public static final String BASE_PATH = "/api/v1/product-suppliers";

    private final IProductFournisseurService productFournisseurService;

    public ProductFournisseurController(IProductFournisseurService productFournisseurService) {
        this.productFournisseurService = productFournisseurService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    public ResponseEntity<ProductFournisseurResponse> create(@Valid @RequestBody ProductFournisseurRequest productFournisseurRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productFournisseurService.create(productFournisseurRequest));
    }

    @PostMapping("/find-or-create")
    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    public ResponseEntity<ProductFournisseurResponse> findOrCreate(@Valid @RequestBody ProductFournisseurRequest request) {
        return ResponseEntity.ok(productFournisseurService.findOrCreate(request));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<Page<ProductFournisseurResponse>> search(@RequestParam(required = false) String q,
                                                                   Pageable pageable) {
        return ResponseEntity.ok(productFournisseurService.search(q, pageable));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<Page<ProductFournisseurResponse>> list(@RequestParam(required = false) UUID productId,
                                                                 Pageable pageable) {
        Page<ProductFournisseurResponse> result = productId != null
                ? productFournisseurService.findAllByProductId(productId, pageable)
                : productFournisseurService.findAllByCurrentEntreprise(pageable);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<ProductFournisseurResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productFournisseurService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    public ResponseEntity<ProductFournisseurResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody ProductFournisseurRequest productFournisseurRequest) {
        return ResponseEntity.ok(productFournisseurService.update(id, productFournisseurRequest));
    }

    @PutMapping("/{id}/prix-vente")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    public ResponseEntity<ProductFournisseurResponse> updatePrixVente(@PathVariable UUID id,
                                                                       @Valid @RequestBody ProductFournisseurPrixVenteRequest prixVenteRequest) {
        return ResponseEntity.ok(productFournisseurService.updatePrixVente(id, prixVenteRequest.prixVente()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productFournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
