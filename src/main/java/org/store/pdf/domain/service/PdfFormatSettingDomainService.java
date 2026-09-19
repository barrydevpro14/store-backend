package org.store.pdf.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.EntityException;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.model.PdfFormatSetting;
import org.store.pdf.domain.repository.PdfFormatSettingRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Opérations de domaine sur les données de paramétrage PDF (largeur, marges, tailles de police).
 */
@Service
public class PdfFormatSettingDomainService {

    private final PdfFormatSettingRepository repository;

    public PdfFormatSettingDomainService(PdfFormatSettingRepository repository) {
        this.repository = repository;
    }

    public Optional<PdfFormatSetting> findByFormatAndMagasin(UUID pdfFormatConfigId, UUID magasinId) {
        return repository.findByPdfFormatConfigIdAndMagasinId(pdfFormatConfigId, magasinId);
    }

    public PdfFormatSetting findGlobalByFormat(UUID pdfFormatConfigId) {
        return repository.findByPdfFormatConfigIdAndMagasinIsNull(pdfFormatConfigId)
                .orElseThrow(() -> new EntityException("pdfFormatSetting.notFound"));
    }

    /** Resolves the magasin-specific override for a format, falling back to the global default when none exists. */
    public PdfFormatSetting findEffective(UUID pdfFormatConfigId, UUID magasinId) {
        return findEffectiveOptional(pdfFormatConfigId, magasinId)
                .orElseThrow(() -> new EntityException("pdfFormatSetting.notFound"));
    }

    /** Same resolution as {@link #findEffective}, without throwing when no row exists at all (defensive listing use). */
    public Optional<PdfFormatSetting> findEffectiveOptional(UUID pdfFormatConfigId, UUID magasinId) {
        if (magasinId == null) {
            return repository.findByPdfFormatConfigIdAndMagasinIsNull(pdfFormatConfigId);
        }

        return findByFormatAndMagasin(pdfFormatConfigId, magasinId)
                .or(() -> repository.findByPdfFormatConfigIdAndMagasinIsNull(pdfFormatConfigId));
    }

    public List<PdfFormatSetting> findAllByMagasin(UUID magasinId) {
        return repository.findByMagasinId(magasinId);
    }

    /** Creates or updates the (format, magasin) override row from the given request. */
    public PdfFormatSetting createOrUpdateOverride(PdfFormatConfig pdfFormatConfig, Magasin magasin, PdfFormatSettingRequest pdfFormatSettingRequest) {
        PdfFormatSetting pdfFormatSetting = findByFormatAndMagasin(pdfFormatConfig.getId(), magasin.getId())
                .orElseGet(PdfFormatSetting::new);

        pdfFormatSetting.setPdfFormatConfig(pdfFormatConfig);
        pdfFormatSetting.setMagasin(magasin);
        pdfFormatSetting.setPageWidth(pdfFormatSettingRequest.pageWidth());
        pdfFormatSetting.setPageHeight(pdfFormatSettingRequest.pageHeight());
        pdfFormatSetting.setMarginLeft(pdfFormatSettingRequest.marginLeft());
        pdfFormatSetting.setMarginRight(pdfFormatSettingRequest.marginRight());
        pdfFormatSetting.setMarginTop(pdfFormatSettingRequest.marginTop());
        pdfFormatSetting.setMarginBottom(pdfFormatSettingRequest.marginBottom());
        pdfFormatSetting.setFontSizeTitle(pdfFormatSettingRequest.fontSizeTitle());
        pdfFormatSetting.setFontSizeNormal(pdfFormatSettingRequest.fontSizeNormal());
        pdfFormatSetting.setFontSizeSmall(pdfFormatSettingRequest.fontSizeSmall());

        return repository.save(pdfFormatSetting);
    }

    public void delete(PdfFormatSetting pdfFormatSetting) {
        repository.delete(pdfFormatSetting);
    }
}
