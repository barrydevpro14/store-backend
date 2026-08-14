package org.store.pdf.infrastructure.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.repository.PdfFormatConfigRepository;

import java.util.UUID;

@Repository
public interface PdfFormatConfigJpaRepository
        extends JpaRepository<PdfFormatConfig, UUID>, PdfFormatConfigRepository {
}
