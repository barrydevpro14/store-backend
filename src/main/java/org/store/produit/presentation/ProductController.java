package org.store.produit.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.RequestParam;
import org.store.common.dto.ImageDownloadResponse;
import org.store.produit.application.dto.ImageMetadataResponse;
import org.store.produit.application.dto.ProductFilter;
import org.store.produit.application.dto.ProductRequest;
import org.store.produit.application.dto.ProductResponse;
import org.store.produit.application.dto.ProductSearchResponse;
import org.store.produit.application.service.IProductSearchService;
import org.store.produit.application.service.IProductService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ProductController.BASE_PATH)
public class ProductController {

    public static final String BASE_PATH = "/api/v1/products";

    private final IProductService productService;
    private final IProductSearchService productSearchService;

    public ProductController(IProductService productService, IProductSearchService productSearchService) {
        this.productService = productService;
        this.productSearchService = productSearchService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PRODUCT_CREATE')")
    public ResponseEntity<ProductResponse> create(@Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.create(productRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<Page<ProductResponse>> list(@RequestParam(required = false) String nom,
                                                      @RequestParam(required = false) String reference,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productService.findAll(
                new ProductFilter(nom, reference, startDate, endDate, page, size)));
    }

    @GetMapping("/search")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<Page<ProductSearchResponse>> search(@RequestParam(value = "q", required = false) String searchTerm,
                                                              @RequestParam(required = false) UUID magasinId,
                                                              @RequestParam(defaultValue = "0") int page,
                                                              @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(productSearchService.search(searchTerm, magasinId, PageRequest.of(page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<ProductResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_UPDATE')")
    public ResponseEntity<ProductResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody ProductRequest productRequest) {
        return ResponseEntity.ok(productService.update(id, productRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PRODUCT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        productService.delete(id);
        return ResponseEntity.noContent().build();
    }

    @PutMapping(value = "/{id}/image", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_UPLOAD_IMAGE')")
    public ResponseEntity<ProductResponse> uploadImagePrincipal(@PathVariable UUID id,
                                                                @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(productService.uploadImagePrincipal(id, file));
    }

    @DeleteMapping("/{id}/image")
    @PreAuthorize("hasAuthority('PRODUCT_UPLOAD_IMAGE')")
    public ResponseEntity<Void> deleteImagePrincipal(@PathVariable UUID id) {
        productService.deleteImagePrincipal(id);
        return ResponseEntity.noContent().build();
    }

    @PostMapping(value = "/{id}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('PRODUCT_UPLOAD_IMAGE')")
    public ResponseEntity<List<UUID>> uploadImages(@PathVariable UUID id,
                                                   @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.status(HttpStatus.CREATED).body(productService.uploadImages(id, files));
    }

    @GetMapping("/{id}/image")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<byte[]> viewImagePrincipal(@PathVariable UUID id) {
        ImageDownloadResponse download = productService.getImagePrincipal(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @GetMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<byte[]> viewImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        ImageDownloadResponse download = productService.getImage(id, imageId);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}/images/{imageId}")
    @PreAuthorize("hasAuthority('PRODUCT_UPLOAD_IMAGE')")
    public ResponseEntity<Void> deleteImage(@PathVariable UUID id, @PathVariable UUID imageId) {
        productService.deleteImage(id, imageId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/{id}/images")
    @PreAuthorize("hasAuthority('PRODUCT_READ')")
    public ResponseEntity<List<ImageMetadataResponse>> listImages(@PathVariable UUID id) {
        return ResponseEntity.ok(productService.listImages(id));
    }
}
