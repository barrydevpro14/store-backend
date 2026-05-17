package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.common.dto.ImageDownloadResponse;

import java.util.UUID;

@RestController
@RequestMapping(PaiementAbonnementController.BASE_PATH)
public class PaiementAbonnementController {

    public static final String BASE_PATH = "/api/v1/paiements-abonnement";

    private final IPaiementAbonnementService paiementAbonnementService;

    public PaiementAbonnementController(IPaiementAbonnementService paiementAbonnementService) {
        this.paiementAbonnementService = paiementAbonnementService;
    }

    @PostMapping(value = "/abonnements/{abonnementId}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PAY')")
    public ResponseEntity<PaiementAbonnementResponse> create(@PathVariable UUID abonnementId,
                                                             @RequestPart("data") @Valid PaiementAbonnementRequest paiementAbonnementRequest,
                                                             @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(paiementAbonnementService.create(abonnementId, paiementAbonnementRequest, file));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<Page<PaiementAbonnementResponse>> list(@RequestParam(required = false) String statut,
                                                                 @RequestParam(required = false) UUID abonnementId,
                                                                 @RequestParam(required = false) UUID entrepriseId,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paiementAbonnementService.findAll(
                new PaiementAbonnementFilter(statut, abonnementId, entrepriseId, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<PaiementAbonnementResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paiementAbonnementService.findResponseById(id));
    }

    @GetMapping("/{id}/preuve")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<byte[]> getPreuve(@PathVariable UUID id) {
        ImageDownloadResponse download = paiementAbonnementService.getPreuve(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VALIDATE')")
    public ResponseEntity<PaiementAbonnementResponse> validate(@PathVariable UUID id) {
        return ResponseEntity.ok(paiementAbonnementService.validate(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VALIDATE')")
    public ResponseEntity<PaiementAbonnementResponse> reject(@PathVariable UUID id,
                                                             @Valid @RequestBody RejectPaiementRequest rejectPaiementRequest) {
        return ResponseEntity.ok(paiementAbonnementService.reject(id, rejectPaiementRequest));
    }
}
