package org.store.achat.presentation;

import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.store.achat.application.dto.AchatRequest;
import org.store.achat.application.dto.AchatResponse;
import org.store.achat.application.service.IAchatService;

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
    public ResponseEntity<AchatResponse> create(@Valid @RequestBody AchatRequest achatRequest) {
        return ResponseEntity.status(HttpStatus.CREATED).body(achatService.create(achatRequest));
    }
}
