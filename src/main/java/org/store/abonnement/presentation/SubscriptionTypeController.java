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
import org.store.abonnement.application.dto.SubscriptionTypeFilter;
import org.store.abonnement.application.dto.SubscriptionTypeRequest;
import org.store.abonnement.application.dto.SubscriptionTypeResponse;
import org.store.abonnement.application.service.ISubscriptionTypeService;

import java.util.UUID;

/**
 * Endpoints nested sous Plan : `/api/v1/plans/{planId}/types`. Les types
 * (durées) sont scopés par plan ; le planId vient du path et n'est
 * pas passé dans le body.
 */
@RestController
@RequestMapping(SubscriptionTypeController.BASE_PATH)
public class SubscriptionTypeController {

    public static final String BASE_PATH = "/api/v1/plans/{planId}/types";

    private final ISubscriptionTypeService subscriptionTypeService;

    public SubscriptionTypeController(ISubscriptionTypeService subscriptionTypeService) {
        this.subscriptionTypeService = subscriptionTypeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('TYPE_PLAN_CREATE')")
    public ResponseEntity<SubscriptionTypeResponse> create(@PathVariable UUID planId,
                                                           @Valid @RequestBody SubscriptionTypeRequest subscriptionTypeRequest) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(subscriptionTypeService.create(planId, subscriptionTypeRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('TYPE_PLAN_READ')")
    public ResponseEntity<Page<SubscriptionTypeResponse>> list(@PathVariable UUID planId,
                                                               @RequestParam(required = false) String nom,
                                                               @RequestParam(required = false) Boolean actif,
                                                               @RequestParam(required = false) Boolean recommande,
                                                               @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(subscriptionTypeService.findAll(
                planId, new SubscriptionTypeFilter(nom, actif, recommande, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('TYPE_PLAN_READ')")
    public ResponseEntity<SubscriptionTypeResponse> getById(@PathVariable UUID planId, @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.findResponseById(planId, id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('TYPE_PLAN_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> update(@PathVariable UUID planId,
                                                           @PathVariable UUID id,
                                                           @Valid @RequestBody SubscriptionTypeRequest subscriptionTypeRequest) {
        return ResponseEntity.ok(subscriptionTypeService.update(planId, id, subscriptionTypeRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('TYPE_PLAN_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> activate(@PathVariable UUID planId, @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.activate(planId, id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('TYPE_PLAN_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> deactivate(@PathVariable UUID planId, @PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.deactivate(planId, id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('TYPE_PLAN_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID planId, @PathVariable UUID id) {
        subscriptionTypeService.delete(planId, id);
        return ResponseEntity.noContent().build();
    }
}
