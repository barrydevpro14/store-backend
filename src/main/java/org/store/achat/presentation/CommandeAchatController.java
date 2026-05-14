package org.store.achat.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.service.ICommandeAchatService;

import java.util.UUID;

@RestController
@RequestMapping(CommandeAchatController.BASE_PATH)
public class CommandeAchatController {

    public static final String BASE_PATH = "/api/v1/commandes-achat";

    private final ICommandeAchatService commandeAchatService;

    public CommandeAchatController(ICommandeAchatService commandeAchatService) {
        this.commandeAchatService = commandeAchatService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<Page<CommandeAchatResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID fournisseurId,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commandeAchatService.findAllByCurrentEntreprise(
                new CommandeAchatFilter(magasinId, fournisseurId, startDate, endDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<CommandeAchatResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(commandeAchatService.findResponseById(id));
    }
}
