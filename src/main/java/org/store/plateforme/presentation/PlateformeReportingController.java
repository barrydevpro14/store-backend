package org.store.plateforme.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;
import org.store.plateforme.application.service.IPlateformeReportingService;

import java.util.UUID;

@RestController
@RequestMapping(PlateformeReportingController.BASE_PATH)
public class PlateformeReportingController {

    public static final String BASE_PATH = "/api/v1/admin/plateforme/reporting";

    private final IPlateformeReportingService service;

    public PlateformeReportingController(IPlateformeReportingService service) {
        this.service = service;
    }

    @GetMapping("/period")
    @PreAuthorize("hasAuthority('PLATFORM_REPORT_READ')")
    public ResponseEntity<PlateformePeriodReportResponse> period(@RequestParam(required = false) String startDate,
                                                                  @RequestParam(required = false) String endDate,
                                                                  @RequestParam(required = false) UUID countryId,
                                                                  @RequestParam(required = false) UUID abonnementId) {
        return ResponseEntity.ok(service.getPeriodReport(new PlateformePeriodFilter(startDate, endDate, countryId, abonnementId)));
    }
}
