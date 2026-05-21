package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
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
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.abonnement.application.dto.PlanAbonnementFilter;
import org.store.abonnement.application.dto.PlanAbonnementRequest;
import org.store.abonnement.application.dto.PlanAbonnementResponse;
import org.store.abonnement.application.service.IPlanAbonnementService;

import java.util.UUID;

@RestController
@RequestMapping(PlanAbonnementController.BASE_PATH)
public class PlanAbonnementController {

    public static final String BASE_PATH = "/api/v1/plans";

    private final IPlanAbonnementService planAbonnementService;

    public PlanAbonnementController(IPlanAbonnementService planAbonnementService) {
        this.planAbonnementService = planAbonnementService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PLAN_CREATE')")
    public ResponseEntity<PlanAbonnementResponse> create(@Valid @RequestBody PlanAbonnementRequest planAbonnementRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(planAbonnementService.create(planAbonnementRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PLAN_READ')")
    public ResponseEntity<Page<PlanAbonnementResponse>> list(@RequestParam(required = false) String nom,
                                                             @RequestParam(required = false) Boolean actif,
                                                             @RequestParam(required = false) Boolean visible,
                                                             @RequestParam(required = false) Boolean trial,
                                                             @RequestParam(defaultValue = "0") int page,
                                                             @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(planAbonnementService.findAll(
                new PlanAbonnementFilter(nom, actif, visible, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PLAN_READ')")
    public ResponseEntity<PlanAbonnementResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(planAbonnementService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PLAN_UPDATE')")
    public ResponseEntity<PlanAbonnementResponse> update(@PathVariable UUID id,
                                                         @Valid @RequestBody PlanAbonnementRequest planAbonnementRequest) {
        return ResponseEntity.ok(planAbonnementService.update(id, planAbonnementRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PLAN_UPDATE')")
    public ResponseEntity<PlanAbonnementResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(planAbonnementService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PLAN_UPDATE')")
    public ResponseEntity<PlanAbonnementResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(planAbonnementService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PLAN_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        planAbonnementService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
