package org.store.magasin.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.service.IMagasinService;

import java.util.UUID;

@RestController
@RequestMapping(MagasinController.BASE_PATH)
@PreAuthorize("hasAuthority('PROPRIETAIRE_ACCESS')")
public class MagasinController {

    public static final String BASE_PATH = "/api/v1/magasins";

    private final IMagasinService magasinService;

    public MagasinController(IMagasinService magasinService) {
        this.magasinService = magasinService;
    }

    @PostMapping
    public ResponseEntity<MagasinResponse> create(@Valid @RequestBody MagasinRequest magasinRequest) {
        MagasinResponse response = magasinService.create(magasinRequest);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @GetMapping
    public ResponseEntity<Page<MagasinResponse>> list(Pageable pageable) {
        return ResponseEntity.ok(magasinService.findAllByCurrentEntreprise(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<MagasinResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.findResponseById(id));
    }

    @PutMapping("/{id}")
    public ResponseEntity<MagasinResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody MagasinRequest magasinRequest) {
        return ResponseEntity.ok(magasinService.update(id, magasinRequest));
    }

    @PatchMapping("/{id}/activate")
    public ResponseEntity<MagasinResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<MagasinResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.deactivate(id));
    }
}
