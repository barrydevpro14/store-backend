package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.stock.application.dto.MarginReportFilter;
import org.store.stock.application.dto.MarginReportResponse;
import org.store.stock.application.service.IMarginReportService;

import java.util.UUID;

@RestController
@RequestMapping(MarginReportingController.BASE_PATH)
public class MarginReportingController {

    public static final String BASE_PATH = "/api/v1/reports/margins";

    private final IMarginReportService marginReportService;

    public MarginReportingController(IMarginReportService marginReportService) {
        this.marginReportService = marginReportService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('REPORT_STOCK')")
    public ResponseEntity<MarginReportResponse> compute(@RequestParam UUID magasinId,
                                                        @RequestParam(required = false) UUID productId,
                                                        @RequestParam(required = false) UUID fournisseurId,
                                                        @RequestParam(required = false) String startDate,
                                                        @RequestParam(required = false) String endDate) {
        return ResponseEntity.ok(marginReportService.compute(
                new MarginReportFilter(magasinId, productId, fournisseurId, startDate, endDate)
        ));
    }
}
