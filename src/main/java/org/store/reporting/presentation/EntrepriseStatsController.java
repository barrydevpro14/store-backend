package org.store.reporting.presentation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.entreprise.application.dto.EntrepriseStatsResponse;
import org.store.entreprise.application.service.IEntrepriseService;

@RestController
public class EntrepriseStatsController {

    private final IEntrepriseService entrepriseService;

    public EntrepriseStatsController(IEntrepriseService entrepriseService) {
        this.entrepriseService = entrepriseService;
    }

    @GetMapping("/api/v1/entreprises/stats")
    @PreAuthorize("hasAuthority('COMPANY_READ')")
    public ResponseEntity<Page<EntrepriseStatsResponse>> stats(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(entrepriseService.findStats(
                PageRequest.of(page, size, Sort.by(Sort.Direction.ASC, "raisonSociale"))));
    }
}
