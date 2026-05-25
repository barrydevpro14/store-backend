package org.store.vente.presentation;

import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.vente.application.dto.CommandeVenteFilter;
import org.store.vente.application.dto.CommandeVenteResponse;
import org.store.vente.application.service.ICommandeVenteService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping(CommandeVenteController.BASE_PATH)
public class CommandeVenteController {

    public static final String BASE_PATH = "/api/v1/commandes-vente";

    private final ICommandeVenteService commandeVenteService;

    public CommandeVenteController(ICommandeVenteService commandeVenteService) {
        this.commandeVenteService = commandeVenteService;
    }

    @GetMapping
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<Page<CommandeVenteResponse>> list(@RequestParam UUID magasinId,
                                                            @RequestParam(required = false) UUID clientId,
                                                            @RequestParam(required = false) UUID vendeurId,
                                                            @RequestParam(required = false) String statut,
                                                            @RequestParam(required = false) String reference,
                                                            @RequestParam(required = false) BigDecimal montantMin,
                                                            @RequestParam(required = false) BigDecimal montantMax,
                                                            @RequestParam(required = false) String startDate,
                                                            @RequestParam(required = false) String endDate,
                                                            @RequestParam(required = false) LocalDate createdStartDate,
                                                            @RequestParam(required = false) LocalDate createdEndDate,
                                                            @RequestParam(defaultValue = "0") int page,
                                                            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(commandeVenteService.findAllByCurrentEntreprise(
                new CommandeVenteFilter(magasinId, clientId, vendeurId, statut, reference,
                        montantMin, montantMax, startDate, endDate, createdStartDate, createdEndDate, page, size)
        ));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('SALE_READ')")
    public ResponseEntity<CommandeVenteResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(commandeVenteService.findResponseById(id));
    }
}
