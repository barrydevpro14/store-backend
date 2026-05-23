package org.store.abonnement.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
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
import org.store.abonnement.application.dto.PromotionFilter;
import org.store.abonnement.application.dto.PromotionRequest;
import org.store.abonnement.application.dto.PromotionResponse;
import org.store.abonnement.application.service.IPromotionService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(PromotionController.BASE_PATH)
public class PromotionController {

    public static final String BASE_PATH = "/api/v1/promotions";

    private final IPromotionService promotionService;

    public PromotionController(IPromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PROMOTION_CREATE')")
    public ResponseEntity<PromotionResponse> create(@Valid @RequestBody PromotionRequest promotionRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(promotionService.create(promotionRequest));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PROMOTION_READ')")
    public ResponseEntity<Page<PromotionResponse>> list(@RequestParam(required = false) String nom,
                                                        @RequestParam(required = false) Boolean actif,
                                                        @RequestParam(required = false) UUID planId,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdStartDate,
                                                        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate createdEndDate,
                                                        @RequestParam(defaultValue = "0") int page,
                                                        @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(promotionService.findAll(
                new PromotionFilter(nom, actif, planId, createdStartDate, createdEndDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_READ')")
    public ResponseEntity<PromotionResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.findResponseById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public ResponseEntity<PromotionResponse> update(@PathVariable UUID id,
                                                    @Valid @RequestBody PromotionRequest promotionRequest) {
        return ResponseEntity.ok(promotionService.update(id, promotionRequest));
    }

    @PatchMapping("/{id}/activate")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public ResponseEntity<PromotionResponse> activate(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.activate(id));
    }

    @PatchMapping("/{id}/deactivate")
    @PreAuthorize("hasAuthority('PROMOTION_UPDATE')")
    public ResponseEntity<PromotionResponse> deactivate(@PathVariable UUID id) {
        return ResponseEntity.ok(promotionService.deactivate(id));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('PROMOTION_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        promotionService.delete(id);
        return ResponseEntity.noContent().build();
    }
}
