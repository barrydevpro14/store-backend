package org.store.pdf.application.service;

import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.application.dto.PdfFormatSettingResponse;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.util.List;
import java.util.UUID;

public interface IPdfFormatSettingService {

    /** Returns the effective parametrage (override or global) for every enabled format, scoped to the given magasin. */
    List<PdfFormatSettingResponse> findEffectiveForMagasin(UUID magasinId);

    /** Resolves the format catalog entry with its effective numeric parametrage merged in for the given magasin. */
    PdfFormatConfig resolveEffectiveConfig(UUID pdfFormatConfigId, UUID magasinId);

    /** Creates or replaces the magasin-specific override for a format. */
    PdfFormatSettingResponse upsertOverride(UUID pdfFormatConfigId, UUID magasinId, PdfFormatSettingRequest pdfFormatSettingRequest);

    /** Deletes the magasin-specific override, reverting the magasin to the global default. */
    void deleteOverride(UUID pdfFormatConfigId, UUID magasinId);
}
