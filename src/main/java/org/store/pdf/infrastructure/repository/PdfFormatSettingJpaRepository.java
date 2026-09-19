package org.store.pdf.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.pdf.domain.model.PdfFormatSetting;
import org.store.pdf.domain.repository.PdfFormatSettingRepository;

import java.util.UUID;

@Repository
public interface PdfFormatSettingJpaRepository
        extends JpaRepository<PdfFormatSetting, UUID>, PdfFormatSettingRepository {
}
