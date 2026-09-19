package org.store.pdf.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.service.ValidatorService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.application.dto.PdfFormatSettingRequest;
import org.store.pdf.application.dto.PdfFormatSettingResponse;
import org.store.pdf.application.service.impl.PdfFormatSettingServiceImpl;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.model.PdfFormatSetting;
import org.store.pdf.domain.service.PdfFormatSettingDomainService;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PdfFormatSettingServiceImplTest {

    @Mock
    private PdfFormatSettingDomainService domainService;

    @Mock
    private IPdfFormatConfigService pdfFormatConfigService;

    @Mock
    private IMagasinService magasinService;

    @Mock
    private ValidatorService validatorService;

    @InjectMocks
    private PdfFormatSettingServiceImpl service;

    private UUID magasinId;
    private Magasin magasin;
    private PdfFormatConfig thermal58;
    private PdfFormatSetting globalSetting;
    private PdfFormatSetting overrideSetting;

    @BeforeEach
    void setUp() {
        magasinId = UUID.randomUUID();
        magasin = new Magasin();
        magasin.setId(magasinId);

        thermal58 = new PdfFormatConfig();
        thermal58.setId(UUID.randomUUID());
        thermal58.setCode("THERMAL_58MM");
        thermal58.setLabel("Ticket thermique 58mm");
        thermal58.setFormat(PdfFormat.THERMAL_58MM);
        thermal58.setEnabled(true);

        globalSetting = new PdfFormatSetting();
        globalSetting.setPdfFormatConfig(thermal58);
        globalSetting.setPageWidth(BigDecimal.valueOf(136));
        globalSetting.setMarginLeft(BigDecimal.valueOf(4));
        globalSetting.setMarginRight(BigDecimal.valueOf(4));
        globalSetting.setMarginTop(BigDecimal.valueOf(4));
        globalSetting.setMarginBottom(BigDecimal.valueOf(4));
        globalSetting.setFontSizeTitle(BigDecimal.valueOf(10));
        globalSetting.setFontSizeNormal(BigDecimal.valueOf(8));
        globalSetting.setFontSizeSmall(BigDecimal.valueOf(7));

        overrideSetting = new PdfFormatSetting();
        overrideSetting.setPdfFormatConfig(thermal58);
        overrideSetting.setMagasin(magasin);
        overrideSetting.setPageWidth(BigDecimal.valueOf(204));
        overrideSetting.setMarginLeft(BigDecimal.valueOf(5));
        overrideSetting.setMarginRight(BigDecimal.valueOf(5));
        overrideSetting.setMarginTop(BigDecimal.valueOf(5));
        overrideSetting.setMarginBottom(BigDecimal.valueOf(5));
        overrideSetting.setFontSizeTitle(BigDecimal.valueOf(10));
        overrideSetting.setFontSizeNormal(BigDecimal.valueOf(8));
        overrideSetting.setFontSizeSmall(BigDecimal.valueOf(7));
    }

    @Test
    void findEffectiveForMagasin_should_return_one_entry_per_enabled_format() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(pdfFormatConfigService.findAllEnabledEntities()).thenReturn(List.of(thermal58));
        when(domainService.findEffectiveOptional(thermal58.getId(), magasinId)).thenReturn(Optional.of(overrideSetting));

        List<PdfFormatSettingResponse> result = service.findEffectiveForMagasin(magasinId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).overridden()).isTrue();
        assertThat(result.get(0).pageWidth()).isEqualByComparingTo(BigDecimal.valueOf(204));
    }

    @Test
    void findEffectiveForMagasin_should_skip_format_with_no_resolvable_setting() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(pdfFormatConfigService.findAllEnabledEntities()).thenReturn(List.of(thermal58));
        when(domainService.findEffectiveOptional(thermal58.getId(), magasinId)).thenReturn(Optional.empty());

        List<PdfFormatSettingResponse> result = service.findEffectiveForMagasin(magasinId);

        assertThat(result).isEmpty();
    }

    @Test
    void resolveEffectiveConfig_should_merge_catalog_and_effective_setting() {
        when(pdfFormatConfigService.findById(thermal58.getId())).thenReturn(thermal58);
        when(domainService.findEffective(thermal58.getId(), magasinId)).thenReturn(globalSetting);

        PdfFormatConfig result = service.resolveEffectiveConfig(thermal58.getId(), magasinId);

        assertThat(result.getFormat()).isEqualTo(PdfFormat.THERMAL_58MM);
        assertThat(result.getPageWidth()).isEqualByComparingTo(BigDecimal.valueOf(136));
    }

    @Test
    void upsertOverride_should_validate_and_delegate_to_domain_service() {
        PdfFormatSettingRequest request = new PdfFormatSettingRequest(
                BigDecimal.valueOf(204), null,
                BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5), BigDecimal.valueOf(5),
                BigDecimal.valueOf(10), BigDecimal.valueOf(8), BigDecimal.valueOf(7));

        when(pdfFormatConfigService.findById(thermal58.getId())).thenReturn(thermal58);
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.createOrUpdateOverride(thermal58, magasin, request)).thenReturn(overrideSetting);

        PdfFormatSettingResponse result = service.upsertOverride(thermal58.getId(), magasinId, request);

        verify(validatorService).validate(request);
        assertThat(result.overridden()).isTrue();
        assertThat(result.pageWidth()).isEqualByComparingTo(BigDecimal.valueOf(204));
    }

    @Test
    void deleteOverride_should_delete_when_override_exists() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.findByFormatAndMagasin(thermal58.getId(), magasinId)).thenReturn(Optional.of(overrideSetting));

        service.deleteOverride(thermal58.getId(), magasinId);

        verify(domainService).delete(overrideSetting);
    }

    @Test
    void deleteOverride_should_be_idempotent_when_no_override_exists() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(domainService.findByFormatAndMagasin(thermal58.getId(), magasinId)).thenReturn(Optional.empty());

        service.deleteOverride(thermal58.getId(), magasinId);

        verify(domainService, never()).delete(any());
    }
}
