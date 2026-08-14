package org.store.pdf.application.service;

import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.util.List;
import java.util.UUID;

public interface IPdfFormatConfigService {

    /** Retourne toutes les configurations activées — pour le sélecteur frontend. */
    List<PdfFormatConfigResponse> findAll();

    /** Résout la configuration par UUID — lève EntityException si absente ou désactivée. */
    PdfFormatConfig findById(UUID id);
}
