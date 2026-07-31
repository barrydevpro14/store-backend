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
import org.store.magasin.application.dto.MagasinCountResponse;
import org.store.magasin.application.dto.MagasinFilter;
import org.store.magasin.application.dto.MagasinRequest;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.application.dto.MagasinStatsResponse;
import org.store.magasin.application.dto.MagasinSummaryResponse;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.application.service.IMagasinStatsService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(MagasinController.BASE_PATH)
public class MagasinController {

    public static final String BASE_PATH = "/api/v1/magasins";

    private final IMagasinService magasinService;
    private final IMagasinStatsService magasinStatsService;

    public MagasinController(IMagasinService magasinService,
                             IMagasinStatsService magasinStatsService) {
        this.magasinService = magasinService;
        this.magasinStatsService = magasinStatsService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('STORE_CREATE')")
    public ResponseEntity<MagasinResponse> create(@Valid @RequestBody MagasinRequest magasinRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(magasinService.create(magasinRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_READ')")
    public ResponseEntity<Page<MagasinResponse>> list(@RequestParam(required = false) String nom,
                                                      @RequestParam(required = false) Boolean actif,
                                                      @RequestParam(required = false) String startDate,
                                                      @RequestParam(required = false) String endDate,
                                                      @RequestParam(defaultValue = "0") int page,
                                                      @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(magasinService.findAllByCurrentEntreprise(
                new MagasinFilter(nom, actif, startDate, endDate, page, size)));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('STORE_READ')")
    public ResponseEntity<MagasinCountResponse> count() {
        return ResponseEntity.ok(magasinService.countByCurrentEntreprise());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<MagasinResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.findResponseById(id));
    }

    @GetMapping("/{id}/summary")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<MagasinSummaryResponse> getMagasinById(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.findEmployeById(id));
    }

    @GetMapping("/{id}/stats")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<MagasinStatsResponse> stats(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinStatsService.getStats(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> update(@PathVariable UUID id,
                                                  @Valid @RequestBody MagasinRequest magasinRequest) {
        return ResponseEntity.ok(magasinService.update(id, magasinRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(magasinService.deactivate(id));
    }

    @PutMapping(value = "/{id}/logo", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<MagasinResponse> uploadLogo(@PathVariable UUID id,
                                                      @RequestPart("file") MultipartFile file) {
        return ResponseEntity.ok(magasinService.uploadLogo(id, file));
    }

    @GetMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<byte[]> getLogo(@PathVariable UUID id) {
        ImageDownloadResponse download = magasinService.getLogo(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @DeleteMapping("/{id}/logo")
    @PreAuthorize("hasAuthority('STORE_UPDATE')")
    public ResponseEntity<Void> deleteLogo(@PathVariable UUID id) {
        magasinService.deleteLogo(id);
        return ResponseEntity.noContent().build();
    }

    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    @GetMapping("/active/entreprise")
    public ResponseEntity<List<MagasinSummaryResponse>> getActiveEntreprise() {
        return ResponseEntity.ok(magasinService.findAllByCurrentEntreprise());
    }
}
