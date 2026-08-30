package org.store.paiement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.paiement.application.dto.FacturationFilter;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(FacturationController.BASE_PATH)
public class FacturationController {

    public static final String BASE_PATH = "/api/v1/facturations";

    private final IFacturationService service;

    public FacturationController(IFacturationService service) {
        this.service = service;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('FACTURATION_READ')")
    public ResponseEntity<Page<FacturationResponse>> list(@RequestParam(required = false) UUID moyenPaiementId,
                                                            @RequestParam(required = false) UUID paysId,
                                                            @RequestParam(required = false) String numeroFacturation,
                                                            @RequestParam(required = false) Boolean actif,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        FacturationFilter filter = new FacturationFilter(moyenPaiementId, paysId, numeroFacturation, actif, createdStartDate, createdEndDate, page, size);
        return ResponseEntity.ok(service.findAll(filter));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_READ')")
    public ResponseEntity<FacturationResponse> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('FACTURATION_CREATE')")
    public ResponseEntity<FacturationResponse> create(@Valid @RequestBody FacturationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody FacturationRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('FACTURATION_UPDATE')")
    public ResponseEntity<FacturationResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(service.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('FACTURATION_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
