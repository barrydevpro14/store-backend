package org.store.paiement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.common.dto.DataSelect;
import org.store.paiement.application.dto.MoyenPaiementRequest;
import org.store.paiement.application.dto.MoyenPaiementResponse;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.application.service.IMoyenPaiementService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(MoyenPaiementController.BASE_PATH)
public class MoyenPaiementController {

    public static final String BASE_PATH = "/api/v1/moyens-paiement";

    private final IMoyenPaiementService service;

    public MoyenPaiementController(IMoyenPaiementService service) {
        this.service = service;
    }

    @GetMapping
    public ResponseEntity<List<MoyenPaiementResponse>> list() {
        return ResponseEntity.ok(service.findAll());
    }

    @GetMapping("/select")
    public ResponseEntity<Page<DataSelect>> select(@RequestParam(required = false) UUID countryId,
                                                     @RequestParam(value = "q", required = false) String searchTerm,
                                                     @RequestParam(defaultValue = "0") int page,
                                                     @RequestParam(defaultValue = "10") int size) {
        MoyenPaiementSelectFilter filter = new MoyenPaiementSelectFilter(countryId, searchTerm, page, size);
        return ResponseEntity.ok(service.findSelectItems(filter));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('MOYEN_PAIEMENT_CREATE')")
    public ResponseEntity<MoyenPaiementResponse> create(@Valid @RequestBody MoyenPaiementRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('MOYEN_PAIEMENT_UPDATE')")
    public ResponseEntity<MoyenPaiementResponse> update(@PathVariable UUID id,
                                                        @Valid @RequestBody MoyenPaiementRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('MOYEN_PAIEMENT_UPDATE')")
    public ResponseEntity<MoyenPaiementResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('MOYEN_PAIEMENT_UPDATE')")
    public ResponseEntity<MoyenPaiementResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('MOYEN_PAIEMENT_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
