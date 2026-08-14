package org.store.pdf.domain.repository;

import org.springframework.data.jpa.repository.Query;
import org.store.common.repository.BaseRepository;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.util.List;

public interface PdfFormatConfigRepository extends BaseRepository<PdfFormatConfig> {

    @Query("SELECT new org.store.pdf.application.dto.PdfFormatConfigResponse(c) FROM PdfFormatConfig c WHERE c.enabled = true ORDER BY c.label ASC")
    List<PdfFormatConfigResponse> findAllEnabled();
}
