package org.store.pdf.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.EntityException;
import org.store.pdf.application.dto.PdfFormatConfigResponse;
import org.store.pdf.application.service.impl.PdfFormatConfigServiceImpl;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.service.PdfFormatConfigDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PdfFormatConfigServiceImplTest {

    @Mock
    private PdfFormatConfigDomainService domainService;

    @InjectMocks
    private PdfFormatConfigServiceImpl service;

    private UUID configId;
    private PdfFormatConfig enabledConfig;

    @BeforeEach
    void setUp() {
        configId = UUID.randomUUID();
        enabledConfig = new PdfFormatConfig();
        enabledConfig.setId(configId);
        enabledConfig.setCode("A4");
        enabledConfig.setLabel("A4");
        enabledConfig.setFormat(PdfFormat.A4);
        enabledConfig.setEnabled(true);
    }

    @Test
    void findAll_should_delegate_to_domain_service() {
        List<PdfFormatConfigResponse> expected = List.of(new PdfFormatConfigResponse(enabledConfig));
        when(domainService.findAllEnabled()).thenReturn(expected);

        List<PdfFormatConfigResponse> result = service.findAll();

        assertThat(result).isEqualTo(expected);
    }

    @Test
    void findById_should_return_config_when_enabled() {
        when(domainService.findById(configId)).thenReturn(enabledConfig);

        PdfFormatConfig result = service.findById(configId);

        assertThat(result.getId()).isEqualTo(configId);
        assertThat(result.getFormat()).isEqualTo(PdfFormat.A4);
    }

    @Test
    void findById_should_throw_when_config_disabled() {
        enabledConfig.setEnabled(false);
        when(domainService.findById(configId)).thenReturn(enabledConfig);

        assertThatThrownBy(() -> service.findById(configId))
                .isInstanceOf(EntityException.class);
    }

    @Test
    void findById_should_propagate_entity_exception_when_not_found() {
        when(domainService.findById(configId)).thenThrow(new EntityException("pdfFormatConfig.notFound", configId));

        assertThatThrownBy(() -> service.findById(configId))
                .isInstanceOf(EntityException.class);
    }
}
