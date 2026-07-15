package org.store.sequence.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.sequence.application.dto.DocumentSequenceFilter;
import org.store.sequence.application.dto.DocumentSequenceResponse;
import org.store.sequence.domain.enums.TypeDocument;
import org.store.sequence.domain.model.DocumentSequence;
import org.store.sequence.domain.repository.DocumentSequenceRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class DocumentSequenceDomainService extends GlobalService<DocumentSequence, DocumentSequenceRepository> {

    public DocumentSequenceDomainService(DocumentSequenceRepository repository) {
        super(repository);
    }

    /** Verrouille pessimistement la ligne pour génération atomique de référence. */
    public Optional<DocumentSequence> findForUpdate(UUID magasinId, TypeDocument typeDocument) {
        return repository.findForUpdate(magasinId, typeDocument);
    }

    /** Vérifie l'unicité (magasinId, typeDocument) avant création. */
    public boolean existsByMagasinIdAndTypeDocument(UUID magasinId, TypeDocument typeDocument) {
        return repository.existsByMagasinIdAndTypeDocument(magasinId, typeDocument);
    }

    /** Listing paginé filtré par magasin, type de document et dates de création. */
    public Page<DocumentSequenceResponse> findResponsesByFilter(DocumentSequenceFilter filter) {
        return repository.findResponsesByFilter(
                filter.magasinId(),
                filter.typeDocumentAsEnum(),
                filter.startDate(),
                filter.endDate(),
                filter.toPageable()
        );
    }
}
