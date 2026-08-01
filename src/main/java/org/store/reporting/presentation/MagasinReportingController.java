package org.store.reporting.presentation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.reporting.application.dto.MagasinDashboardStatsResponse;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.dto.MagasinVentesStatsResponse;
import org.store.reporting.application.service.IMagasinReportingService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(MagasinReportingController.BASE_PATH)
public class MagasinReportingController {

    public static final String BASE_PATH      = "/api/v1/reporting";
    public static final String OVERVIEW_PATH  = BASE_PATH + "/magasin-overview";
    public static final String DASHBOARD_PATH = BASE_PATH + "/magasin-dashboard";
    public static final String VENTES_PATH    = BASE_PATH + "/magasin-ventes";

    private final IMagasinReportingService magasinReportingService;

    public MagasinReportingController(IMagasinReportingService magasinReportingService) {
        this.magasinReportingService = magasinReportingService;
    }

    @GetMapping("/magasin-overview")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<MagasinOverviewStatsResponse> overview(
            @RequestParam UUID magasinId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        MagasinOverviewFilter magasinOverviewFilter = new MagasinOverviewFilter(magasinId, startDate, endDate);

        return ResponseEntity.ok(magasinReportingService.getOverview(magasinOverviewFilter));
    }

    @GetMapping("/magasin-ventes")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<MagasinVentesStatsResponse> ventesStats(
            @RequestParam UUID magasinId,
            @RequestParam("from") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam("to") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        MagasinOverviewFilter filter = new MagasinOverviewFilter(magasinId, startDate, endDate);

        return ResponseEntity.ok(magasinReportingService.getVentesStats(filter));
    }

    @GetMapping("/magasin-dashboard")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<MagasinDashboardStatsResponse> dashboard(@RequestParam UUID magasinId) {
        return ResponseEntity.ok(magasinReportingService.getDashboardStats(magasinId));
    }
}
