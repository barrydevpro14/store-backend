package org.store.pdf.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.repository.PdfFormatConfigRepository;

import java.util.List;
import java.util.UUID;

/**
 * Opérations de domaine sur les configurations de format PDF.
 */
@Service
public class PdfFormatConfigDomainService {

    private final PdfFormatConfigRepository repository;

    public PdfFormatConfigDomainService(PdfFormatConfigRepository repository) {
        this.repository = repository;
    }

    public PdfFormatConfig findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityException("pdfFormatConfig.notFound", id));
    }

    public List<PdfFormatConfigResponse> findAllEnabled() {
        return repository.findAllEnabled();
    }
}
