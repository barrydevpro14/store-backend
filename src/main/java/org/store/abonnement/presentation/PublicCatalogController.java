package org.store.abonnement.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.abonnement.application.dto.PublicCatalogResponse;
import org.store.abonnement.application.service.IPublicCatalogService;

@RestController
@RequestMapping(PublicCatalogController.BASE_PATH)
public class PublicCatalogController {

    public static final String BASE_PATH = "/api/v1/catalog";

    private final IPublicCatalogService publicCatalogService;

    public PublicCatalogController(IPublicCatalogService publicCatalogService) {
        this.publicCatalogService = publicCatalogService;
    }

    @GetMapping("/public")
    public ResponseEntity<PublicCatalogResponse> findCatalog() {
        return ResponseEntity.ok(publicCatalogService.findCatalog());
    }

    @GetMapping("/subscribable")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_CREATE')")
    public ResponseEntity<PublicCatalogResponse> findSubscribableCatalog() {
        return ResponseEntity.ok(publicCatalogService.findSubscribableCatalog());
    }
}
