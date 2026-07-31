package org.store.reporting.presentation;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.service.IMagasinReportingService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(MagasinReportingController.BASE_PATH)
public class MagasinReportingController {

    public static final String BASE_PATH = "/api/v1/reporting/magasin-overview";

    private final IMagasinReportingService magasinReportingService;

    public MagasinReportingController(IMagasinReportingService magasinReportingService) {
        this.magasinReportingService = magasinReportingService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<MagasinOverviewStatsResponse> overview(
            @RequestParam UUID magasinId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate) {

        MagasinOverviewFilter magasinOverviewFilter = new MagasinOverviewFilter(magasinId, startDate, endDate);

        return ResponseEntity.ok(magasinReportingService.getOverview(magasinOverviewFilter));
    }
}
