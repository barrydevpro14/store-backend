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
import org.springframework.web.bind.annotation.RestController;
import org.store.produit.application.dto.CategoryProductRequest;
import org.store.produit.application.dto.CategoryProductResponse;
import org.store.produit.application.service.ICategoryProductService;

import java.util.UUID;

@RestController
@RequestMapping(CategoryProductController.BASE_PATH)
public class CategoryProductController {

    public static final String BASE_PATH = "/api/v1/category-products";

    private final ICategoryProductService categoryProductService;

    public CategoryProductController(ICategoryProductService categoryProductService) {
        this.categoryProductService = categoryProductService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('CATEGORY_PRODUCT_CREATE')")
    public ResponseEntity<CategoryProductResponse> create(@Valid @RequestBody CategoryProductRequest categoryProductRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(categoryProductService.create(categoryProductRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATEGORY_PRODUCT_READ')")
    public ResponseEntity<Page<CategoryProductResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(categoryProductService.findAllByCurrentEntreprise(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_PRODUCT_READ')")
    public ResponseEntity<CategoryProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(categoryProductService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_PRODUCT_UPDATE')")
    public ResponseEntity<CategoryProductResponse> update(@PathVariable UUID id,
                                                          @Valid @RequestBody CategoryProductRequest categoryProductRequest) {
        return ResponseEntity.ok(categoryProductService.update(id, categoryProductRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATEGORY_PRODUCT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        categoryProductService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
