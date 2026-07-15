package org.store.sequence.application.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.common.tools.EnumHelper;
import org.store.common.validation.DatePattern;
import org.store.common.validation.EnumValue;
import org.store.sequence.domain.enums.TypeDocument;

import java.util.UUID;

public record DocumentSequenceFilter(
        @NotNull UUID magasinId,
        @EnumValue(enumClass = TypeDocument.class) String typeDocument,
        @DatePattern String startDate,
        @DatePattern String endDate,
        @Min(0) int page,
        @Min(1) int size
) {
    public TypeDocument typeDocumentAsEnum() {
        return EnumHelper.parse(TypeDocument.class, typeDocument);
    }

    public Pageable toPageable() {
        return PageRequest.of(page, size);
    }
}
