package org.store.activite.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.activite.application.dto.ActiviteEconomiqueRequest;
import org.store.activite.application.dto.ActiviteEconomiqueResponse;
import org.store.activite.application.dto.ActiviteEconomiqueSummaryResponse;
import org.store.activite.application.service.IActiviteEconomiqueService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(ActiviteEconomiqueController.BASE_PATH)
public class ActiviteEconomiqueController {

    public static final String BASE_PATH = "/api/v1/activites-economiques";

    private final IActiviteEconomiqueService activiteEconomiqueService;

    public ActiviteEconomiqueController(IActiviteEconomiqueService activiteEconomiqueService) {
        this.activiteEconomiqueService = activiteEconomiqueService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_CREATE')")
    public ResponseEntity<ActiviteEconomiqueResponse> create(@Valid @RequestBody ActiviteEconomiqueRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(activiteEconomiqueService.create(request));
    }

    @GetMapping
    public ResponseEntity<List<ActiviteEconomiqueResponse>> list() {
        return ResponseEntity.ok(activiteEconomiqueService.findAll());
    }

    @GetMapping("/actives")
    public ResponseEntity<List<ActiviteEconomiqueSummaryResponse>> listActives() {
        return ResponseEntity.ok(activiteEconomiqueService.findAllActive());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_READ')")
    public ResponseEntity<ActiviteEconomiqueResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(activiteEconomiqueService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_UPDATE')")
    public ResponseEntity<ActiviteEconomiqueResponse> update(@PathVariable UUID id,
                                                             @Valid @RequestBody ActiviteEconomiqueRequest request) {
        return ResponseEntity.ok(activiteEconomiqueService.update(id, request));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_UPDATE')")
    public ResponseEntity<ActiviteEconomiqueResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(activiteEconomiqueService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_UPDATE')")
    public ResponseEntity<ActiviteEconomiqueResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(activiteEconomiqueService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('ACTIVITE_ECONOMIQUE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        activiteEconomiqueService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
