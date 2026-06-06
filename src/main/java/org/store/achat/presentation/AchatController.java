package org.store.achat.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.application.dto.AchatDetailsResponse;
import org.store.achat.application.dto.AchatDraftResponse;
import org.store.achat.application.dto.LigneAchatRequest;
import org.store.achat.application.dto.AchatReceiveRequest;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.dto.AnnulationAchatRequest;
import org.store.achat.application.dto.AnnulationAchatResponse;
import org.store.achat.application.dto.LigneAchatUpdateRequest;
import org.store.achat.application.dto.LigneCommandeAchatResponse;
import org.store.achat.application.service.IAchatService;

import java.util.UUID;

@RestController
@RequestMapping(AchatController.BASE_PATH)
public class AchatController {

    public static final String BASE_PATH = "/api/v1/achats";

    private final IAchatService achatService;

    public AchatController(IAchatService achatService) {
        this.achatService = achatService;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('PURCHASE_CREATE')")
    public ResponseEntity<AchatDraftResponse> create(@Valid @RequestBody AchatRequest achatRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatService.create(achatRequest));
    }

    @GetMapping("/{commandeId}")
    @PreAuthorize("hasAuthority('PURCHASE_READ')")
    public ResponseEntity<AchatDetailsResponse> findDetailsById(@PathVariable UUID commandeId) {
        return ResponseEntity.ok(achatService.findDetailsById(commandeId));
    }

    @DeleteMapping("/{commandeId}")
    @PreAuthorize("hasAuthority('PURCHASE_DELETE')")
    public ResponseEntity<Void> deleteDraft(@PathVariable UUID commandeId) {
        achatService.deleteDraft(commandeId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commandeId}/receive")
    @PreAuthorize("hasAuthority('PURCHASE_APPROVE')")
    public ResponseEntity<AchatResponse> receive(@PathVariable UUID commandeId,
                                                 @Valid @RequestBody AchatReceiveRequest achatReceiveRequest) {
        return ResponseEntity.ok(achatService.receive(commandeId, achatReceiveRequest));
    }

    @GetMapping("/orders/{commandeId}/lignes")
    @PreAuthorize("hasAuthority('PURCHASE_UPDATE')")
    public ResponseEntity<Page<LigneCommandeAchatResponse>> findLignes(
            @PathVariable UUID commandeId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(achatService.findLignesByCommandeId(commandeId, page, size));
    }

    @PostMapping("/orders/{commandeId}/lignes")
    @PreAuthorize("hasAuthority('PURCHASE_UPDATE')")
    public ResponseEntity<LigneCommandeAchatResponse> addLigne(@PathVariable UUID commandeId,
                                                               @Valid @RequestBody LigneAchatRequest ligneAchatRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatService.addLigne(commandeId, ligneAchatRequest));
    }

    @PutMapping("/orders/{commandeId}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('PURCHASE_UPDATE')")
    public ResponseEntity<LigneCommandeAchatResponse> updateLigne(@PathVariable UUID commandeId,
                                                                  @PathVariable UUID ligneId,
                                                                  @Valid @RequestBody LigneAchatUpdateRequest ligneAchatUpdateRequest) {
        return ResponseEntity.ok(achatService.updateLigne(commandeId, ligneId, ligneAchatUpdateRequest));
    }

    @DeleteMapping("/orders/{commandeId}/lignes/{ligneId}")
    @PreAuthorize("hasAuthority('PURCHASE_UPDATE')")
    public ResponseEntity<Void> deleteLigne(@PathVariable UUID commandeId,
                                            @PathVariable UUID ligneId) {
        achatService.deleteLigne(commandeId, ligneId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{commandeId}/annuler")
    @PreAuthorize("hasAuthority('PURCHASE_CANCEL')")
    public ResponseEntity<AnnulationAchatResponse> cancel(@PathVariable UUID commandeId,
                                                          @Valid @RequestBody AnnulationAchatRequest annulationAchatRequest) {
        return ResponseEntity.ok(achatService.cancel(commandeId, annulationAchatRequest));
    }
}
