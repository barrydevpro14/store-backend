package org.store.sequence.application.dto;

import org.store.sequence.domain.enums.TypeDocument;
import org.store.sequence.domain.model.DocumentSequence;

import java.time.LocalDateTime;
import java.util.UUID;

public record DocumentSequenceResponse(
        UUID id,
        UUID magasinId,
        TypeDocument typeDocument,
        String prefixe,
        long prochaineSequence,
        int longueurSequence,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
    public DocumentSequenceResponse(DocumentSequence seq) {
        this(
                seq.getId(),
                seq.getMagasinId(),
                seq.getTypeDocument(),
                seq.getPrefixe(),
                seq.getProchaineSequence(),
                seq.getLongueurSequence(),
                seq.getCreatedAt(),
                seq.getUpdatedAt()
        );
    }
}
