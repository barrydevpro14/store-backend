package org.store.notification.presentation;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.store.common.exceptions.EntityException;
import org.store.notification.application.dto.AlerteResponse;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.notification.domain.service.AlerteDomainService;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;

@RestController
@RequestMapping(AlerteController.BASE_PATH)
public class AlerteController {

    public static final String BASE_PATH = "/api/v1/alertes";

    private final AlerteDomainService alerteDomainService;
    private final ICurrentUserService currentUserService;

    public AlerteController(AlerteDomainService alerteDomainService,
                            ICurrentUserService currentUserService) {
        this.alerteDomainService = alerteDomainService;
        this.currentUserService  = currentUserService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<Page<AlerteResponse>> list(
            @RequestParam(required = false) UUID magasinId,
            @RequestParam(required = false) AlerteType type,
            @RequestParam(required = false) AlerteStatut statut,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "0")  int page,
            @RequestParam(defaultValue = "20") int size) {

        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        LocalDateTime fromDt = from != null ? from.atStartOfDay() : null;
        LocalDateTime toDt   = to   != null ? to.plusDays(1).atStartOfDay() : null;

        return ResponseEntity.ok(alerteDomainService.findByFilter(
                entrepriseId, magasinId, type, statut, fromDt, toDt, page, size));
    }

    @GetMapping("/count")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<Long> countNouvelles() {
        UUID entrepriseId = currentUserService.getCurrent().entrepriseId();
        long count = alerteDomainService
                .findByFilter(entrepriseId, null, null, AlerteStatut.NOUVELLE, null, null, 0, 1)
                .getTotalElements();
        return ResponseEntity.ok(count);
    }

    @PatchMapping("/{id}/lue")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<AlerteResponse> markAsRead(@PathVariable UUID id) {
        var alerte = alerteDomainService.findById(id);
        return ResponseEntity.ok(new AlerteResponse(alerteDomainService.markAsRead(alerte)));
    }

    @PatchMapping("/{id}/resolue")
    @PreAuthorize("hasAuthority('STORE_READ_ONE')")
    public ResponseEntity<AlerteResponse> markAsResolved(@PathVariable UUID id) {
        var alerte = alerteDomainService.findById(id);
        return ResponseEntity.ok(new AlerteResponse(alerteDomainService.markAsResolved(alerte)));
    }
}
