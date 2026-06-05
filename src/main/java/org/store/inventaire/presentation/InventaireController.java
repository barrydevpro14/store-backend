package org.store.inventaire.presentation;

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
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.CloturerRequest;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireUpdateRequest;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.application.service.IInventaireService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(InventaireController.BASE_PATH)
public class InventaireController {

    public static final String BASE_PATH = "/api/v1/inventaires";

    private final IInventaireService inventaireService;

    public InventaireController(IInventaireService inventaireService) {
        this.inventaireService = inventaireService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('INVENTORY_CREATE')")
    public ResponseEntity<InventaireResponse> create(@RequestParam UUID magasinId) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventaireService.create(magasinId));
    }

    @PostMapping("/{id}/lignes")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<LigneInventaireResponse> addLigne(@PathVariable UUID id,
                                                            @Valid @RequestBody LigneInventaireRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(inventaireService.addLigne(id, request));
    }

    @GetMapping("/{id}/lignes")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<Page<LigneInventaireResponse>> listLignes(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(inventaireService.findLignes(id, pageable));
    }

    @PutMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<LigneInventaireResponse> updateLigne(@PathVariable UUID id,
                                                               @PathVariable UUID ligneId,
                                                               @Valid @RequestBody LigneInventaireUpdateRequest request) {
        return ResponseEntity.ok(inventaireService.updateLigne(id, ligneId, request));
    }

    @DeleteMapping("/{id}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('INVENTORY_WRITE')")
    public ResponseEntity<Void> deleteLigne(@PathVariable UUID id, @PathVariable UUID ligneId) {
        inventaireService.deleteLigne(id, ligneId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/bilan")
    @PreAuthorize("hasAuthority('INVENTORY_BILAN')")
    public ResponseEntity<InventaireResponse> passerEnBilan(@PathVariable UUID id,
                                                            @Valid @RequestBody BilanInventaireRequest request) {
        return ResponseEntity.ok(inventaireService.passerEnBilan(id, request));
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('INVENTORY_CLOSE')")
    public ResponseEntity<InventaireResponse> cloturer(@PathVariable UUID id,
                                                       @RequestBody(required = false) CloturerRequest request) {
        return ResponseEntity.ok(inventaireService.cloturer(id, request));
    }

    @GetMapping("/{id}/rapport")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<RapportInventaireResponse> getRapport(@PathVariable UUID id) {
        return ResponseEntity.ok(inventaireService.findRapportByInventaireId(id));
    }

    @PostMapping("/{id}/annuler")
    @PreAuthorize("hasAuthority('INVENTORY_CANCEL')")
    public ResponseEntity<InventaireResponse> annuler(@PathVariable UUID id) {
        return ResponseEntity.ok(inventaireService.annuler(id));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<Page<InventaireResponse>> list(@RequestParam UUID magasinId,
                                                         @RequestParam(required = false) String statut,
                                                         @RequestParam(required = false) String startDate,
                                                         @RequestParam(required = false) String endDate,
                                                         @RequestParam(required = false) LocalDate createdStartDate,
                                                         @RequestParam(required = false) LocalDate createdEndDate,
                                                         @RequestParam(defaultValue = "0") int page,
                                                         @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(inventaireService.findAllByCurrentEntreprise(
                new InventaireFilter(magasinId, statut, startDate, endDate,
                        createdStartDate, createdEndDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('INVENTORY_READ')")
    public ResponseEntity<InventaireResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(inventaireService.findResponseById(id));
    }
}
