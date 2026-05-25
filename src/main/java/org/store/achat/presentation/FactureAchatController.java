package org.store.achat.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.dto.PaiementAchatResponse;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.application.service.IPaiementAchatService;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(FactureAchatController.BASE_PATH)
public class FactureAchatController {

    public static final String BASE_PATH = "/api/v1/factures-achat";

    private final IFactureAchatService factureAchatService;
    private final IPaiementAchatService paiementAchatService;

    public FactureAchatController(IFactureAchatService factureAchatService,
                                  IPaiementAchatService paiementAchatService) {
        this.factureAchatService = factureAchatService;
        this.paiementAchatService = paiementAchatService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<FactureAchatResponse>> list(@RequestParam UUID magasinId,
                                                           @RequestParam(required = false) UUID fournisseurId,
                                                           @RequestParam(required = false) String statut,
                                                           @RequestParam(required = false) String startDate,
                                                           @RequestParam(required = false) String endDate,
                                                           @RequestParam(required = false) LocalDate createdStartDate,
                                                           @RequestParam(required = false) LocalDate createdEndDate,
                                                           @RequestParam(defaultValue = "0") int page,
                                                           @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(factureAchatService.findAllByCurrentEntreprise(
                new FactureAchatFilter(magasinId, fournisseurId, statut, startDate, endDate,
                        createdStartDate, createdEndDate, page, size)
        ));
    }

    @GetMapping("/echeances")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<FactureAchatResponse>> echeances(@RequestParam UUID magasinId,
                                                                @RequestParam(required = false) String startDate,
                                                                @RequestParam(required = false) String endDate,
                                                                @RequestParam(defaultValue = "0") int page,
                                                                @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(factureAchatService.findEcheances(
                new FactureAchatEcheanceFilter(magasinId, startDate, endDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<FactureAchatResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(factureAchatService.findResponseById(id));
    }

    @PostMapping("/{id}/paiements")
    @PreAuthorize("hasAuthority('PURCHASE_PAY')")
    public ResponseEntity<PaiementAchatResponse> createPaiement(@PathVariable UUID id,
                                                                @Valid @RequestBody PaiementAchatRequest paiementAchatRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementAchatService.create(id, paiementAchatRequest));
    }

    @GetMapping("/{id}/paiements")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<PaiementAchatResponse>> listPaiements(@PathVariable UUID id, Pageable pageable) {
        return ResponseEntity.ok(paiementAchatService.findByFactureId(id, pageable));
    }
}
