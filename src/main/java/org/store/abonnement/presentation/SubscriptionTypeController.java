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

@RestController
@RequestMapping(SubscriptionTypeController.BASE_PATH)
public class SubscriptionTypeController {

    public static final String BASE_PATH = "/api/v1/subscription-types";

    private final ISubscriptionTypeService subscriptionTypeService;

    public SubscriptionTypeController(ISubscriptionTypeService subscriptionTypeService) {
        this.subscriptionTypeService = subscriptionTypeService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_CREATE')")
    public ResponseEntity<SubscriptionTypeResponse> create(@Valid @RequestBody SubscriptionTypeRequest subscriptionTypeRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(subscriptionTypeService.create(subscriptionTypeRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_READ')")
    public ResponseEntity<Page<SubscriptionTypeResponse>> list(@RequestParam(required = false) String nom,
                                                               @RequestParam(required = false) Boolean actif,
                                                               @RequestParam(required = false) Boolean recommande,
                                                               @RequestParam(defaultValue = "0") int page,
                                                               @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(subscriptionTypeService.findAll(
                new SubscriptionTypeFilter(nom, actif, recommande, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_READ')")
    public ResponseEntity<SubscriptionTypeResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody SubscriptionTypeRequest subscriptionTypeRequest) {
        return ResponseEntity.ok(subscriptionTypeService.update(id, subscriptionTypeRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_UPDATE')")
    public ResponseEntity<SubscriptionTypeResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(subscriptionTypeService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('SUBSCRIPTION_TYPE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        subscriptionTypeService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
