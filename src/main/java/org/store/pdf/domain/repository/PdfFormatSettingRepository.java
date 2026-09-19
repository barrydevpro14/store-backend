package org.store.pdf.domain.repository;

import org.store.common.repository.BaseRepository;
import org.store.pdf.domain.model.PdfFormatSetting;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PdfFormatSettingRepository extends BaseRepository<PdfFormatSetting> {

    Optional<PdfFormatSetting> findByPdfFormatConfigIdAndMagasinId(UUID pdfFormatConfigId, UUID magasinId);

    Optional<PdfFormatSetting> findByPdfFormatConfigIdAndMagasinIsNull(UUID pdfFormatConfigId);

    List<PdfFormatSetting> findByMagasinId(UUID magasinId);
}
