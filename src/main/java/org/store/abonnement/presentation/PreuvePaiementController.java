package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.service.IPreuvePaiementService;
import org.store.common.dto.ImageDownloadResponse;

import java.util.UUID;

@RestController
@RequestMapping(PreuvePaiementController.BASE_PATH)
public class PreuvePaiementController {

    public static final String BASE_PATH = "/api/v1/preuves-paiement";

    private final IPreuvePaiementService preuvePaiementService;

    public PreuvePaiementController(IPreuvePaiementService preuvePaiementService) {
        this.preuvePaiementService = preuvePaiementService;
    }

    @PatchMapping("/{id}/validate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VALIDATE')")
    public ResponseEntity<PreuvePaiementResponse> validate(@PathVariable UUID id) {
        return ResponseEntity.ok(preuvePaiementService.validate(id));
    }

    @PatchMapping("/{id}/reject")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_VALIDATE')")
    public ResponseEntity<PreuvePaiementResponse> reject(@PathVariable UUID id,
                                                         @Valid @RequestBody RejectPaiementRequest rejectPaiementRequest) {
        return ResponseEntity.ok(preuvePaiementService.reject(id, rejectPaiementRequest));
    }

    @GetMapping("/{id}/preuve")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<byte[]> getPreuve(@PathVariable UUID id) {
        ImageDownloadResponse download = preuvePaiementService.getImage(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }
}
