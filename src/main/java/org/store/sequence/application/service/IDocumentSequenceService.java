package org.store.sequence.application.service;

import org.springframework.data.domain.Page;
import org.store.sequence.application.dto.DocumentSequenceFilter;
import org.store.sequence.application.dto.DocumentSequenceRequest;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.application.dto.DocumentSequenceUpdateRequest;
import org.store.sequence.domain.enums.TypeDocument;

import java.util.UUID;

public interface IDocumentSequenceService {

    /**
     * Génère la prochaine référence pour ce magasin et ce type de document.
     * Fallback vers le format timestamp {@code ReferenceHelper} si aucune séquence n'est configurée.
     */
    String generateReference(UUID magasinId, TypeDocument typeDocument);

    DocumentSequenceResponse create(DocumentSequenceRequest request);

    DocumentSequenceResponse findResponseById(UUID id);

    Page<DocumentSequenceResponse> findAllByFilter(DocumentSequenceFilter filter);

    DocumentSequenceResponse update(UUID id, DocumentSequenceUpdateRequest request);

    void delete(UUID id);
}
