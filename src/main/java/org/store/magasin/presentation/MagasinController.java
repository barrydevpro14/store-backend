package org.store.magasin.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.common.dto.ImageDownloadResponse;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.service.IMagasinService;

import java.util.UUID;

@RestController
@RequestMapping(MagasinController.BASE_PATH)
@PreAuthorize("hasAnyAuthority('PROPRIETAIRE_ACCESS', 'ADMIN_ACCESS')")
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
    public ResponseEntity<Page<MagasinResponse>> list(@RequestParam(required = false) String nom,
                                                      @RequestParam(required = false) Boolean actif,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(magasinService.findAllByCurrentEntreprise(new MagasinFilter(nom, actif, page, size)));
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

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<MagasinResponse> uploadLogo(@PathVariable UUID id,
                                                     @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(magasinService.uploadLogo(id, file));
    }

    @GetMapping("/{id}/logo")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        ImageDownloadResponse download = magasinService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}/logo")
    public ResponseEntity<Void> deleteLogo(@PathVariable UUID id) {
        magasinService.deleteLogo(id);
        return ResponseEntity.noContent().build();
    }
}
