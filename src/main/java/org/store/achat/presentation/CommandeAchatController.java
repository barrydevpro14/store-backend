package org.store.achat.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.service.IBonCommandeAchatPdfService;
import org.store.achat.application.service.ICommandeAchatService;
import org.store.common.dto.DataCountResponse;
import org.store.common.dto.ImageDownloadResponse;

import java.util.UUID;

@RestController
@RequestMapping(CommandeAchatController.BASE_PATH)
public class CommandeAchatController {

    public static final String BASE_PATH = "/api/v1/commandes-achat";

    private final ICommandeAchatService commandeAchatService;
    private final IBonCommandeAchatPdfService bonCommandeAchatPdfService;

    public CommandeAchatController(ICommandeAchatService commandeAchatService,
                                   IBonCommandeAchatPdfService bonCommandeAchatPdfService) {
        this.commandeAchatService = commandeAchatService;
        this.bonCommandeAchatPdfService = bonCommandeAchatPdfService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<CommandeAchatResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID fournisseurId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) String statutFacture,
                                                            @RequestParam(required = false) String reference,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commandeAchatService.findAllByCurrentEntreprise(
                new CommandeAchatFilter(magasinId, fournisseurId, statut, statutFacture, reference, startDate, endDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<CommandeAchatResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(commandeAchatService.findResponseById(id));
    }

    @GetMapping("/pending-purchases/{magasinId}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<DataCountResponse> countDraft(@PathVariable UUID magasinId) {
        return ResponseEntity.ok(commandeAchatService.countDraft(magasinId));
    }

    @GetMapping(value = "/{id}/pdf", produces = MediaType.APPLICATION_PDF_VALUE)
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<byte[]> downloadPdf(@PathVariable UUID id) {
        byte[] pdf = bonCommandeAchatPdfService.generate(id);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"bon-commande-" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdf);
    }

    @GetMapping("/{id}/piece-jointe")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<byte[]> getPieceJointe(@PathVariable UUID id) {
        ImageDownloadResponse download = commandeAchatService.getPieceJointe(id);
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(download.contentType()))
                .body(download.content());
    }
}
