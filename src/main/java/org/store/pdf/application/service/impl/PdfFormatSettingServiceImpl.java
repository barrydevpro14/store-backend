package org.store.pdf.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.application.dto.PdfFormatSettingResponse;
import org.store.pdf.application.service.IPdfFormatConfigService;
import org.store.pdf.application.service.IPdfFormatSettingService;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.model.PdfFormatSetting;
import org.store.pdf.domain.service.PdfFormatSettingDomainService;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Orchestrates resolution and per-magasin customization of PDF parametrage (page width, margins, font sizes).
 */
@Service
@Transactional(readOnly = true)
public class PdfFormatSettingServiceImpl implements IPdfFormatSettingService {

    private final PdfFormatSettingDomainService domainService;
    private final IPdfFormatConfigService pdfFormatConfigService;
    private final IMagasinService magasinService;
    private final ValidatorService validatorService;

    public PdfFormatSettingServiceImpl(PdfFormatSettingDomainService domainService,
                                        IPdfFormatConfigService pdfFormatConfigService,
                                        IMagasinService magasinService,
                                        ValidatorService validatorService) {
        this.domainService = domainService;
        this.pdfFormatConfigService = pdfFormatConfigService;
        this.magasinService = magasinService;
        this.validatorService = validatorService;
    }

    /** Resolves, for every enabled format, the caller's magasin effective parametrage (override or global). */
    @Override
    public List<PdfFormatSettingResponse> findEffectiveForMagasin(UUID magasinId) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));

        return pdfFormatConfigService.findAllEnabledEntities().stream()
                .map(pdfFormatConfig -> resolveSettingResponse(pdfFormatConfig, magasin.getId()))
                .flatMap(Optional::stream)
                .toList();
    }

    /** Resolves one format's effective setting into a response, skipping it when no setting exists at all (defensive). */
    public Optional<PdfFormatSettingResponse> resolveSettingResponse(PdfFormatConfig pdfFormatConfig, UUID magasinId) {
        return domainService.findEffectiveOptional(pdfFormatConfig.getId(), magasinId)
                .map(pdfFormatSetting -> new PdfFormatSettingResponse(pdfFormatConfig, pdfFormatSetting));
    }

    /** Merges the format catalog entry with its effective parametrage for the given magasin. */
    @Override
    public PdfFormatConfig resolveEffectiveConfig(UUID pdfFormatConfigId, UUID magasinId) {
        PdfFormatConfig pdfFormatConfig = pdfFormatConfigService.findById(pdfFormatConfigId);
        PdfFormatSetting pdfFormatSetting = domainService.findEffective(pdfFormatConfigId, magasinId);
        pdfFormatConfig.applySetting(pdfFormatSetting);
        return pdfFormatConfig;
    }

    /** Creates or replaces the magasin-specific override for this format. */
    @Override
    @Transactional
    public PdfFormatSettingResponse upsertOverride(UUID pdfFormatConfigId, UUID magasinId, PdfFormatSettingRequest pdfFormatSettingRequest) {
        validatorService.validate(pdfFormatSettingRequest);
        PdfFormatConfig pdfFormatConfig = pdfFormatConfigService.findById(pdfFormatConfigId);
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));

        PdfFormatSetting saved = domainService.createOrUpdateOverride(pdfFormatConfig, magasin, pdfFormatSettingRequest);
        return new PdfFormatSettingResponse(pdfFormatConfig, saved);
    }

    /** Deletes the magasin's override for this format — reverts to the global parametrage (idempotent). */
    @Override
    @Transactional
    public void deleteOverride(UUID pdfFormatConfigId, UUID magasinId) {
        Magasin magasin = magasinService.ensureAccessibleByCurrentUser(magasinService.findById(magasinId));

        domainService.findByFormatAndMagasin(pdfFormatConfigId, magasin.getId())
                .ifPresent(domainService::delete);
    }
}
