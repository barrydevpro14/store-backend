package org.store.sequence.presentation;

import jakarta.validation.Valid;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.store.sequence.application.dto.DocumentSequenceFilter;
import org.store.sequence.application.dto.DocumentSequenceRequest;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.application.dto.DocumentSequenceUpdateRequest;
import org.store.sequence.application.service.IDocumentSequenceService;

import java.util.UUID;

@RestController
@RequestMapping(DocumentSequenceController.BASE_PATH)
public class DocumentSequenceController {

    public static final String BASE_PATH = "/api/v1/document-sequences";

    private final IDocumentSequenceService service;

    public DocumentSequenceController(IDocumentSequenceService service) {
        this.service = service;
    }

    @PostMapping
    @PreAuthorize("hasAuthority('DOCUMENT_SEQUENCE_CREATE')")
    public ResponseEntity<DocumentSequenceResponse> create(@Valid @RequestBody DocumentSequenceRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.create(request));
    }

    @GetMapping
    @PreAuthorize("hasAuthority('DOCUMENT_SEQUENCE_READ')")
    public ResponseEntity<Page<DocumentSequenceResponse>> list(
            @RequestParam UUID magasinId,
            @RequestParam(required = false) String typeDocument,
            @RequestParam(required = false) String startDate,
            @RequestParam(required = false) String endDate,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        return ResponseEntity.ok(service.findAllByFilter(
                new DocumentSequenceFilter(magasinId, typeDocument, startDate, endDate, page, size)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_SEQUENCE_READ')")
    public ResponseEntity<DocumentSequenceResponse> getById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findResponseById(id));
    }

    @PatchMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_SEQUENCE_UPDATE')")
    public ResponseEntity<DocumentSequenceResponse> update(@PathVariable UUID id,
                                                           @Valid @RequestBody DocumentSequenceUpdateRequest request) {
        return ResponseEntity.ok(service.update(id, request));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAuthority('DOCUMENT_SEQUENCE_DELETE')")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        service.delete(id);
        return ResponseEntity.noContent().build();
    }
}
