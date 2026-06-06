package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.service.IAdminReportingService;

@RestController
@RequestMapping(AdminReportingController.BASE_PATH)
public class AdminReportingController {

    public static final String BASE_PATH = "/api/v1/admin/reporting";

    private final IAdminReportingService adminReportingService;

    public AdminReportingController(IAdminReportingService adminReportingService) {
        this.adminReportingService = adminReportingService;
    }

    @GetMapping("/overview")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public ResponseEntity<AdminOverviewStatsResponse> overview() {
        return ResponseEntity.ok(adminReportingService.getOverviewStats());
    }
}
