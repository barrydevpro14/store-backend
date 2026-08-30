package org.store.paiement.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.application.dto.FacturationResponse;
import org.store.paiement.application.service.IFacturationService;

import java.util.UUID;

@RestController
@RequestMapping(FacturationController.BASE_PATH)
public class FacturationController {

    public static final String BASE_PATH = "/api/v1/facturations";

    private final IFacturationService service;

    public FacturationController(IFacturationService service) {
        this.service = service;
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
