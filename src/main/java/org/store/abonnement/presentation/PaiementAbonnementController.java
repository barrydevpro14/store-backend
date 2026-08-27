package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementDetailsResponse;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.PreuvePaiementRequest;
import org.store.abonnement.application.dto.PreuvePaiementResponse;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IPreuvePaiementService;
import org.store.common.dto.DataCountResponse;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(PaiementAbonnementController.BASE_PATH)
public class PaiementAbonnementController {

    public static final String BASE_PATH = "/api/v1/paiements-abonnement";

    private final IPaiementAbonnementService paiementAbonnementService;
    private final IPreuvePaiementService preuvePaiementService;

    public PaiementAbonnementController(IPaiementAbonnementService paiementAbonnementService,
                                        IPreuvePaiementService preuvePaiementService) {
        this.paiementAbonnementService = paiementAbonnementService;
        this.preuvePaiementService = preuvePaiementService;
    }

    @PostMapping(value = "/{id}/payer", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @PreAuthorize("hasAuthority('SUBSCRIPTION_PAY')")
    public ResponseEntity<PreuvePaiementResponse> payer(@PathVariable UUID id,
                                                        @RequestPart("data") @Valid PreuvePaiementRequest preuvePaiementRequest,
                                                        @RequestPart(value = "file", required = false) MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(preuvePaiementService.create(id, preuvePaiementRequest, file));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<Page<PaiementAbonnementResponse>> list(@RequestParam(required = false) String statut,
                                                                 @RequestParam(required = false) UUID abonnementId,
                                                                 @RequestParam(required = false) UUID entrepriseId,
                                                                 @RequestParam(required = false) String startDate,
                                                                 @RequestParam(required = false) String endDate,
                                                                 @RequestParam(defaultValue = "0") int page,
                                                                 @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(paiementAbonnementService.findAll(
                new PaiementAbonnementFilter(statut, abonnementId, entrepriseId, startDate, endDate, page, size)));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<DataCountResponse> count(@RequestParam(required = false) String statut,
                                                   @RequestParam(required = false) LocalDate startDate,
                                                   @RequestParam(required = false) LocalDate endDate) {
        return ResponseEntity.ok(new DataCountResponse(
                paiementAbonnementService.countByStatutAndCreatedBetween(statut, startDate, endDate)));
    }

    @GetMapping("/me/pending")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<PaiementAbonnementDetailsResponse> findMyPending() {
        return paiementAbonnementService.findMyPending()
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.noContent().build());
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<PaiementAbonnementDetailsResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(paiementAbonnementService.findDetailsById(id));
    }
}
