package org.store.vente.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.AnnulationVenteRequest;
import org.store.vente.application.dto.AnnulationVenteResponse;
import org.store.vente.application.dto.VenteDetailsResponse;
import org.store.vente.application.dto.VenteRequest;
import org.store.vente.application.dto.VenteResponse;
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
    public ResponseEntity<VenteResponse> create(@Valid @RequestBody VenteRequest venteRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(venteService.create(venteRequest));
    }

    @GetMapping("/{commandeId}")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<VenteDetailsResponse> findDetailsById(@PathVariable UUID commandeId) {
        return ResponseEntity.ok(venteService.findDetailsById(commandeId));
    }

    @PostMapping("/{commandeId}/annuler")
    @PreAuthorize("hasAuthority('SALE_CANCEL')")
    public ResponseEntity<AnnulationVenteResponse> cancel(@PathVariable UUID commandeId,
                                                          @Valid @RequestBody AnnulationVenteRequest annulationVenteRequest) {
        return ResponseEntity.ok(venteService.cancel(commandeId, annulationVenteRequest));
    }
}
