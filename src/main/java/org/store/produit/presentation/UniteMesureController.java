package org.store.produit.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.application.dto.UniteMesureSummaryResponse;
import org.store.produit.application.service.IUniteMesureService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(UniteMesureController.BASE_PATH)
public class UniteMesureController {

    public static final String BASE_PATH = "/api/v1/unites-mesure";

    private final IUniteMesureService uniteMesureService;

    public UniteMesureController(IUniteMesureService uniteMesureService) {
        this.uniteMesureService = uniteMesureService;
    }

    @GetMapping("/all")
    public ResponseEntity<List<UniteMesureSummaryResponse>> listAll() {
        return ResponseEntity.ok(uniteMesureService.listAll());
    }

    @PostMapping
    @PreAuthorize("hasAuthority('UNITE_MESURE_CREATE')")
    public ResponseEntity<UniteMesureResponse> create(@Valid @RequestBody UniteMesureRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(uniteMesureService.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('UNITE_MESURE_READ')")
    public ResponseEntity<Page<UniteMesureResponse>> list(
            @RequestParam(required = false) String libelle,
            @RequestParam(required = false) String code,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(uniteMesureService.findAll(
                new UniteMesureFilter(libelle, code, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('UNITE_MESURE_READ')")
    public ResponseEntity<UniteMesureResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(uniteMesureService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('UNITE_MESURE_UPDATE')")
    public ResponseEntity<UniteMesureResponse> update(@PathVariable UUID id,
                                                       @Valid @RequestBody UniteMesureRequest request) {
        return ResponseEntity.ok(uniteMesureService.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('UNITE_MESURE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        uniteMesureService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
