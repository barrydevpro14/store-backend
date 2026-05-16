package org.store.vente.presentation;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IPaiementVenteService;

import java.util.UUID;

@RestController
@RequestMapping(FactureClientController.BASE_PATH)
public class FactureClientController {

    public static final String BASE_PATH = "/api/v1/factures-client";

    private final IFactureClientService factureClientService;
    private final IPaiementVenteService paiementVenteService;

    public FactureClientController(IFactureClientService factureClientService,
                                   IPaiementVenteService paiementVenteService) {
        this.factureClientService = factureClientService;
        this.paiementVenteService = paiementVenteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<Page<FactureClientResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID clientId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(factureClientService.findAllByCurrentEntreprise(
                new FactureClientFilter(magasinId, clientId, statut, startDate, endDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<FactureClientResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(factureClientService.findResponseById(id));
    }

    @GetMapping("/{id}/paiements")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<Page<PaiementVenteResponse>> listPaiements(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(paiementVenteService.findByFactureId(id, pageable));
    }
}
