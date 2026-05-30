package org.store.vente.presentation;

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
import org.store.vente.application.dto.FactureClientFilter;
import org.store.vente.application.dto.FactureClientResponse;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.application.service.IInvoicePdfService;
import org.store.vente.application.service.IPaiementVenteService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(FactureClientController.BASE_PATH)
public class FactureClientController {

    public static final String BASE_PATH = "/api/v1/factures-client";

    private final IFactureClientService factureClientService;
    private final IPaiementVenteService paiementVenteService;
    private final IInvoicePdfService invoicePdfService;

    public FactureClientController(IFactureClientService factureClientService,
                                   IPaiementVenteService paiementVenteService,
                                   IInvoicePdfService invoicePdfService) {
        this.factureClientService = factureClientService;
        this.paiementVenteService = paiementVenteService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<Page<FactureClientResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID clientId,
                                                            @RequestParam(required = false) UUID vendeurId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) String numero,
                                                            @RequestParam(required = false) BigDecimal montantMin,
                                                            @RequestParam(required = false) BigDecimal montantMax,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(required = false) LocalDate createdStartDate,
                                                            @RequestParam(required = false) LocalDate createdEndDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(factureClientService.findAllByCurrentEntreprise(
                new FactureClientFilter(magasinId, clientId, vendeurId, statut, numero,
                        montantMin, montantMax, startDate, endDate, createdStartDate, createdEndDate, page, size)
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

    @PostMapping("/{id}/paiements")
    @PreAuthorize("hasAuthority('SALE_PAY')")
    public ResponseEntity<PaiementVenteResponse> createPaiement(@PathVariable UUID id,
                                                                @Valid @RequestBody PaiementVenteRequest paiementVenteRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(paiementVenteService.create(id, paiementVenteRequest));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdf = invoicePdfService.generateFactureClientPdf(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"facture-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }
}
