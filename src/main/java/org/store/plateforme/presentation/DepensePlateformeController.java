package org.store.plateforme.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.application.service.IDepensePlateformeService;

import java.util.UUID;

@RestController
@RequestMapping(DepensePlateformeController.BASE_PATH)
public class DepensePlateformeController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/depenses";

    private final IDepensePlateformeService service;

    public DepensePlateformeController(IDepensePlateformeService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_CREATE')")
    public ResponseEntity<DepensePlateformeResponse> create(@Valid @RequestBody DepensePlateformeRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<Page<DepensePlateformeResponse>> list(@RequestParam(required = false) UUID categoryId,
                                                                @RequestParam(required = false) UUID moyenPaiementId,
                                                                @RequestParam(required = false) UUID countryId,
                                                                @RequestParam(required = false) Boolean actif,
                                                                @RequestParam(required = false) String libelle,
                                                                @RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findAll(
                new DepensePlateformeFilter(categoryId, moyenPaiementId, countryId, actif, libelle, startDate, endDate, page, size)));
    }

    @GetMapping("/total")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<DepensePlateformeTotalResponse> computeTotal(@RequestParam(required = false) UUID categoryId,
                                                                       @RequestParam(required = false) UUID moyenPaiementId,
                                                                       @RequestParam(required = false) UUID countryId,
                                                                       @RequestParam(required = false) String libelle,
                                                                       @RequestParam(required = false) String startDate,
                                                                       @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(service.computeTotal(
                new DepensePlateformeFilter(categoryId, moyenPaiementId, countryId, null, libelle, startDate, endDate, 0, 1)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_READ')")
    public ResponseEntity<DepensePlateformeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_UPDATE')")
    public ResponseEntity<DepensePlateformeResponse> update(@PathVariable UUID id,
                                                            @Valid @RequestBody DepensePlateformeRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLATFORM_EXPENSE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
