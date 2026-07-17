package org.store.catalogue.presentation;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.store.catalogue.application.dto.CatalogueImportResult;
import org.store.catalogue.application.dto.CatalogueProduitFilter;
import org.store.catalogue.application.dto.CatalogueProduitResponse;
import org.store.catalogue.application.dto.CatalogueProduitSummaryResponse;
import org.store.catalogue.application.dto.CatalogueProduitUpdateRequest;
import org.store.catalogue.application.service.ICatalogueProduitService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(CatalogueProduitController.BASE_PATH)
public class CatalogueProduitController {

    public static final String BASE_PATH = "/api/v1/catalogue";

    private final ICatalogueProduitService catalogueProduitService;

    public CatalogueProduitController(ICatalogueProduitService catalogueProduitService) {
        this.catalogueProduitService = catalogueProduitService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('CATALOGUE_PRODUIT_READ')")
    public ResponseEntity<List<CatalogueProduitSummaryResponse>> list() {
        return ResponseEntity.ok(catalogueProduitService.findByCurrentEntreprise());
    }

    @GetMapping("/by-activite")
    @PreAuthorize("hasAuthority('CATALOGUE_PRODUIT_READ')")
    public ResponseEntity<Page<CatalogueProduitSummaryResponse>> listByActivite(
            @RequestParam UUID activiteEconomiqueId,
            @RequestParam(required = false) String reference,
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String categorie,
            @RequestParam(required = false) String createdStartDate,
            @RequestParam(required = false) String createdEndDate,
            @RequestParam(defaultValue = "0") @Min(0) int page,
            @RequestParam(defaultValue = "10") @Min(1) int size
    ) {
        return ResponseEntity.ok(catalogueProduitService.findByFilter(
                new CatalogueProduitFilter(activiteEconomiqueId, reference, libelle, categorie,
                        createdStartDate, createdEndDate, page, size)));
    }

    @PostMapping("/import")
    @PreAuthorize("hasAuthority('CATALOGUE_PRODUIT_IMPORT')")
    public ResponseEntity<CatalogueImportResult> importFromFile(
            @RequestParam("file") MultipartFile file,
            @RequestParam("activiteEconomiqueId") UUID activiteEconomiqueId
    ) {
        return ResponseEntity.ok(catalogueProduitService.importFromFile(file, activiteEconomiqueId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOGUE_PRODUIT_UPDATE')")
    public ResponseEntity<CatalogueProduitResponse> update(
            @PathVariable UUID id,
            @Valid @RequestBody CatalogueProduitUpdateRequest request
    ) {
        return ResponseEntity.ok(catalogueProduitService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('CATALOGUE_PRODUIT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        catalogueProduitService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
