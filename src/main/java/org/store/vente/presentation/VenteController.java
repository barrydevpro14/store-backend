package org.store.vente.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.AnnulationVenteRequest;
import org.store.vente.application.dto.AnnulationVenteResponse;
import org.store.vente.application.dto.LigneCommandeVenteResponse;
import org.store.vente.application.dto.LigneVenteRequest;
import org.store.vente.application.dto.LigneVenteUpdateRequest;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteDraftResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
import org.store.vente.application.dto.VenteValidateRequest;
import org.store.vente.application.service.IVenteService;

import java.util.UUID;

@RestController
@RequestMapping(VenteController.BASE_PATH)
public class VenteController {

    public static final String BASE_PATH = "/api/v1/ventes";

    private final IVenteService venteService;

    public VenteController(IVenteService venteService) {
        this.venteService = venteService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SALE_CREATE')")
    public ResponseEntity<VenteDraftResponse> create(@Valid @RequestBody VenteRequest venteRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venteService.create(venteRequest));
    }

    @GetMapping("/{commandeId}")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<VenteDetailsResponse> findDetailsById(@PathVariable UUID commandeId) {
        return ResponseEntity.ok(venteService.findDetailsById(commandeId));
    }

    @PostMapping("/{commandeId}/validate")
    @PreAuthorize("hasAuthority('SALE_APPROVE')")
    public ResponseEntity<VenteResponse> validate(@PathVariable UUID commandeId,
                                                  @Valid @RequestBody VenteValidateRequest venteValidateRequest) {
        return ResponseEntity.ok(venteService.validate(commandeId, venteValidateRequest));
    }

    @GetMapping("/orders/{commandeId}/lignes")
    @PreAuthorize("hasAuthority('SALE_UPDATE')")
    public ResponseEntity<Page<LigneCommandeVenteResponse>> findLignes(
            @PathVariable UUID commandeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(venteService.findLignesByCommandeId(commandeId, page, size));
    }

    @PostMapping("/orders/{commandeId}/lignes")
    @PreAuthorize("hasAuthority('SALE_UPDATE')")
    public ResponseEntity<LigneCommandeVenteResponse> addLigne(@PathVariable UUID commandeId,
                                                               @Valid @RequestBody LigneVenteRequest ligneVenteRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venteService.addLigne(commandeId, ligneVenteRequest));
    }

    @PutMapping("/orders/{commandeId}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('SALE_UPDATE')")
    public ResponseEntity<LigneCommandeVenteResponse> updateLigne(@PathVariable UUID commandeId,
                                                                  @PathVariable UUID ligneId,
                                                                  @Valid @RequestBody LigneVenteUpdateRequest ligneVenteUpdateRequest) {
        return ResponseEntity.ok(venteService.updateLigne(commandeId, ligneId, ligneVenteUpdateRequest));
    }

    @DeleteMapping("/orders/{commandeId}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('SALE_UPDATE')")
    public ResponseEntity<Void> deleteLigne(@PathVariable UUID commandeId,
                                            @PathVariable UUID ligneId) {
        venteService.deleteLigne(commandeId, ligneId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{commandeId}")
    @PreAuthorize("hasAuthority('SALE_DELETE')")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID commandeId) {
        venteService.deleteDraft(commandeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commandeId}/annuler")
    @PreAuthorize("hasAuthority('SALE_CANCEL')")
    public ResponseEntity<AnnulationVenteResponse> cancel(@PathVariable UUID commandeId,
                                                          @Valid @RequestBody AnnulationVenteRequest annulationVenteRequest) {
        return ResponseEntity.ok(venteService.cancel(commandeId, annulationVenteRequest));
    }
}
