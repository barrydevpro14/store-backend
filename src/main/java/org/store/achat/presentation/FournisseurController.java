package org.store.achat.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.store.achat.application.dto.FournisseurFilter;
import org.store.achat.application.dto.FournisseurRequest;
import org.store.achat.application.dto.FournisseurResponse;
import org.store.achat.application.service.IFournisseurService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(FournisseurController.BASE_PATH)
public class FournisseurController {

    public static final String BASE_PATH = "/api/v1/suppliers";

    private final IFournisseurService fournisseurService;

    public FournisseurController(IFournisseurService fournisseurService) {
        this.fournisseurService = fournisseurService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUPPLIER_CREATE')")
    public ResponseEntity<FournisseurResponse> create(@Valid @RequestBody FournisseurRequest fournisseurRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(fournisseurService.create(fournisseurRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<Page<FournisseurResponse>> list(@RequestParam(required = false) String nom,
                                                          @RequestParam(required = false) String reference,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                          @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                          @RequestParam(defaultValue = "0") int page,
                                                          @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(fournisseurService.findAll(
                new FournisseurFilter(nom, reference, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_READ')")
    public ResponseEntity<FournisseurResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(fournisseurService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_UPDATE')")
    public ResponseEntity<FournisseurResponse> update(@PathVariable UUID id,
                                                      @Valid @RequestBody FournisseurRequest fournisseurRequest) {
        return ResponseEntity.ok(fournisseurService.update(id, fournisseurRequest));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUPPLIER_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        fournisseurService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
