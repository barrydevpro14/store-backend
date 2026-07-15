package org.store.sequence.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.EnumValue;
import org.store.sequence.domain.enums.TypeDocument;

import java.util.UUID;

public record DocumentSequenceRequest(
        @NotNull UUID magasinId,
        @NotNull @EnumValue(enumClass = TypeDocument.class) String typeDocument,
        @NotBlank String prefixe,
        @Min(1) long prochaineSequence,
        @Min(1) int longueurSequence
) {
    public TypeDocument typeDocumentAsEnum() {
        return EnumHelper.parse(TypeDocument.class, typeDocument);
    }
}
