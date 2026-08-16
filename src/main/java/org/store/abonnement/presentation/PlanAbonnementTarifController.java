package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.abonnement.application.dto.PlanAbonnementTarifRequest;
import org.store.abonnement.application.dto.PlanAbonnementTarifResponse;
import org.store.abonnement.application.service.IPlanAbonnementTarifService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(PlanAbonnementTarifController.BASE_PATH)
public class PlanAbonnementTarifController {

    public static final String BASE_PATH = "/api/v1/plans/{planId}/tarifs";

    private final IPlanAbonnementTarifService tarifService;

    public PlanAbonnementTarifController(IPlanAbonnementTarifService tarifService) {
        this.tarifService = tarifService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TARIF_READ')")
    public ResponseEntity<List<PlanAbonnementTarifResponse>> findByPlan(@PathVariable UUID planId) {
        return ResponseEntity.ok(tarifService.findResponsesByPlan(planId));
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TARIF_CREATE')")
    public ResponseEntity<PlanAbonnementTarifResponse> create(@PathVariable UUID planId,
                                                               @Valid @RequestBody PlanAbonnementTarifRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(tarifService.create(planId, request));
    }

    @PutMapping("/{tarifId}")
    @PreAuthorize("hasAuthority('TARIF_UPDATE')")
    public ResponseEntity<PlanAbonnementTarifResponse> update(@PathVariable UUID planId,
                                                               @PathVariable UUID tarifId,
                                                               @Valid @RequestBody PlanAbonnementTarifRequest request) {
        return ResponseEntity.ok(tarifService.update(planId, tarifId, request));
    }

    @DeleteMapping("/{tarifId}")
    @PreAuthorize("hasAuthority('TARIF_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID planId,
                                        @PathVariable UUID tarifId) {
        tarifService.delete(planId, tarifId);
        return ResponseEntity.noContent().build();
    }
}
