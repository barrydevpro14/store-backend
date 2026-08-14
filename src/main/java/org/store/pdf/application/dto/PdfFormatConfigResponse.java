package org.store.pdf.application.dto;

import org.store.pdf.domain.model.PdfFormatConfig;

import java.util.UUID;

public record PdfFormatConfigResponse(UUID id, String label) {

    public PdfFormatConfigResponse(PdfFormatConfig config) {
        this(config.getId(), config.getLabel());
    }
}
