package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.reporting.application.dto.OwnerOverviewStatsResponse;
import org.store.reporting.application.service.IOwnerReportingService;

@RestController
@RequestMapping("/api/v1/reporting")
public class OwnerReportingController {

    private final IOwnerReportingService ownerReportingService;

    public OwnerReportingController(IOwnerReportingService ownerReportingService) {
        this.ownerReportingService = ownerReportingService;
    }

    @GetMapping("/owner-overview")
    @PreAuthorize("hasAuthority('OWNER_ACCESS')")
    public ResponseEntity<OwnerOverviewStatsResponse> getOwnerOverview() {
        return ResponseEntity.ok(ownerReportingService.getOwnerOverviewStats());
    }
}
