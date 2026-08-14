package org.store.pdf.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.exceptions.EntityException;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.application.service.IPdfFormatConfigService;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.service.PdfFormatConfigDomainService;

import java.util.List;
import java.util.UUID;

/**
 * Accès en lecture aux configurations de format PDF utilisées par le frontend
 * et par les services de génération de PDF (facture, bon commande).
 */
@Service
@Transactional(readOnly = true)
public class PdfFormatConfigServiceImpl implements IPdfFormatConfigService {

    private final PdfFormatConfigDomainService domainService;

    public PdfFormatConfigServiceImpl(PdfFormatConfigDomainService domainService) {
        this.domainService = domainService;
    }

    /** Retourne toutes les configurations activées triées par code. */
    @Override
    public List<PdfFormatConfigResponse> findAll() {
        return domainService.findAllEnabled();
    }

    /** Résout l'entité par UUID et vérifie qu'elle est activée. */
    @Override
    public PdfFormatConfig findById(UUID id) {
        PdfFormatConfig config = domainService.findById(id);

        if (!config.isEnabled()) {
            throw new EntityException("pdfFormatConfig.notFound", id);
        }

        return config;
    }
}
