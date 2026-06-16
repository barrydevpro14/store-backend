package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.abonnement.application.dto.AbonnementFilter;
import org.store.abonnement.application.dto.AbonnementResponse;
import org.store.abonnement.application.dto.CurrentAbonnementResponse;
import org.store.abonnement.application.dto.RenouvellementAutoRequest;
import org.store.abonnement.application.dto.SubscribeRequest;
import org.store.abonnement.application.dto.SubscribeResponse;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.security.application.service.ICurrentUserService;

import java.util.UUID;

@RestController
@RequestMapping(AbonnementController.BASE_PATH)
public class AbonnementController {

    public static final String BASE_PATH = "/api/v1/abonnements";

    private final IAbonnementService abonnementService;
    private final ICurrentUserService currentUserService;

    public AbonnementController(IAbonnementService abonnementService,
                                ICurrentUserService currentUserService) {
        this.abonnementService = abonnementService;
        this.currentUserService = currentUserService;
    }

    @PostMapping("/subscribe")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_CREATE')")
    public ResponseEntity<SubscribeResponse> subscribe(@Valid @RequestBody SubscribeRequest subscribeRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(abonnementService.subscribe(subscribeRequest));
    }

    @PatchMapping("/{id}/renouvellement-auto")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_UPDATE')")
    public ResponseEntity<AbonnementResponse> updateRenouvellementAuto(@PathVariable UUID id,
                                                                       @Valid @RequestBody RenouvellementAutoRequest renouvellementAutoRequest) {
        return ResponseEntity.ok(abonnementService.updateRenouvellementAuto(id, renouvellementAutoRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<Page<AbonnementResponse>> findAll(@RequestParam(required = false) UUID entrepriseId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) UUID planId,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(abonnementService.findAll(
                new AbonnementFilter(entrepriseId, statut, planId, startDate, endDate, page, size)));
    }

    @GetMapping("/me")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<Page<AbonnementResponse>> findMyHistory(@RequestParam(required = false) String statut,
                                                                  @RequestParam(required = false) UUID planId,
                                                                  @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                                  @RequestParam(defaultValue = "0") int page,
                                                                  @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(abonnementService.findMyHistory(
                new AbonnementFilter(null, statut, planId, startDate, endDate, page, size)));
    }

    @GetMapping("/me/current")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_READ')")
    public ResponseEntity<CurrentAbonnementResponse> findMyCurrent() {
        // ADMIN has no company → no subscription to return
        if (currentUserService.getCurrent().entrepriseId() == null) {
            return ResponseEntity.noContent().build();
        }
        return ResponseEntity.ok(abonnementService.findMyCurrent());
    }
}
