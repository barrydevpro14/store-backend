package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.magasin.application.dto.MagasinStatsRow;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.dto.PeriodReportResponse;
import org.store.reporting.application.service.IAdminReportingService;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

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

    @GetMapping("/period")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public ResponseEntity<PeriodReportResponse> period(@RequestParam(required = false) String startDate,
                                                       @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(adminReportingService.getPeriodStats(startDate, endDate));
    }

    @GetMapping("/entreprise/{id}")
    @PreAuthorize("hasAuthority('REPORT_FINANCIAL')")
    public ResponseEntity<List<MagasinStatsRow>> entreprise(@PathVariable UUID id) {
        return ResponseEntity.ok(adminReportingService.getEntrepriseStats(id));
    }
}
