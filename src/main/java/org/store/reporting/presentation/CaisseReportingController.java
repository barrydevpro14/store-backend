package org.store.reporting.presentation;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;
import org.store.vente.application.service.ICaisseService;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(CaisseReportingController.BASE_PATH)
public class CaisseReportingController {

    public static final String BASE_PATH = "/api/v1/ventes/caisse";

    private final ICaisseService caisseService;

    public CaisseReportingController(ICaisseService caisseService) {
        this.caisseService = caisseService;
    }

    @GetMapping("/resume")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<CaisseResumeResponse> resume(@RequestParam UUID magasinId,
                                                       @RequestParam String from,
                                                       @RequestParam(required = false) String to) {
        return ResponseEntity.ok(caisseService.getResume(new CaisseResumeFilter(magasinId, from, to)));
    }

    @GetMapping("/top-produits")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<List<TopProduitResponse>> topProduits(@RequestParam UUID magasinId,
                                                                @RequestParam(required = false) String date,
                                                                @RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate,
                                                                @RequestParam(defaultValue = "5") int nombre) {
        return ResponseEntity.ok(caisseService.findTopProduits(
                new TopProduitsFilter(magasinId, date, startDate, endDate, nombre)));
    }
}
