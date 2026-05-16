package org.store.vente.presentation;

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
@RequestMapping(CaisseController.BASE_PATH)
public class CaisseController {

    public static final String BASE_PATH = "/api/v1/ventes/caisse";

    private final ICaisseService caisseService;

    public CaisseController(ICaisseService caisseService) {
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
                                                                @RequestParam(defaultValue = "3") int nombre) {
        return ResponseEntity.ok(caisseService.findTopProduits(new TopProduitsFilter(magasinId, date, nombre)));
    }
}
