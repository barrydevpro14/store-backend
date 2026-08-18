package org.store.vente.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;
import org.store.vente.application.service.IInvoicePdfService;

import java.util.UUID;

@RestController
@RequestMapping(CommandeVenteController.BASE_PATH)
public class CommandeVenteController {

    public static final String BASE_PATH = "/api/v1/commandes-vente";

    private final ICommandeVenteService commandeVenteService;
    private final IInvoicePdfService invoicePdfService;

    public CommandeVenteController(ICommandeVenteService commandeVenteService,
                                   IInvoicePdfService invoicePdfService) {
        this.commandeVenteService = commandeVenteService;
        this.invoicePdfService = invoicePdfService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<Page<CommandeVenteResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID clientId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) String statutFacture,
                                                            @RequestParam(required = false) String reference,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commandeVenteService.findAllByCurrentEntreprise(
                new CommandeVenteFilter(magasinId, clientId, statut, statutFacture, reference,
                        startDate, endDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<CommandeVenteResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(commandeVenteService.findResponseById(id));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id, @RequestParam UUID configId) {
        byte[] pdf = invoicePdfService.generateForCommande(id, configId);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"commande-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @PostMapping("/{id}/cloturer")
    @PreAuthorize("hasAuthority('SALE_UPDATE')")
    public ResponseEntity<?> cloturer(@PathVariable UUID id) {
        commandeVenteService.cloturerCommande(id);
        return ResponseEntity.status(HttpStatus.CREATED).body("ok");
    }
}
