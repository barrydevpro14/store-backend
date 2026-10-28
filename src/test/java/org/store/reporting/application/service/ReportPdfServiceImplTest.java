package org.store.reporting.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.IPdfService;
import org.store.common.service.ValidatorService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseParCategorieResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.application.service.IDepenseService;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.application.service.IPdfFormatSettingService;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.dto.MagasinVentesStatsResponse;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.dto.ReportPdfFilter;
import org.store.reporting.application.dto.ReportPdfLine;
import org.store.reporting.application.pdf.strategy.ReportPdfStrategy;
import org.store.reporting.application.pdf.strategy.ReportPdfStrategyResolver;
import org.store.reporting.application.service.impl.ReportPdfServiceImpl;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.tuple;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReportPdfServiceImplTest {

    @Mock private IMagasinReportingService magasinReportingService;
    @Mock private IDepenseService depenseService;
    @Mock private IMagasinService magasinService;
    @Mock private IPdfFormatSettingService pdfFormatSettingService;
    @Mock private ReportPdfStrategyResolver strategyResolver;
    @Mock private IPdfService pdfService;
    @Mock private ValidatorService validatorService;
    @Mock private ReportPdfStrategy reportPdfStrategy;

    @InjectMocks private ReportPdfServiceImpl reportPdfService;

    private final UUID magasinId = UUID.randomUUID();
    private final UUID configId = UUID.randomUUID();
    private final byte[] pdfBytes = "%PDF".getBytes();

    private Magasin magasin;
    private PdfFormatConfig config;
    private ReportPdfFilter reportPdfFilter;

    @BeforeEach
    void setUp() {
        magasin = new Magasin();
        magasin.setId(magasinId);

        config = new PdfFormatConfig();
        config.setFormat(PdfFormat.A4);

        reportPdfFilter = new ReportPdfFilter(magasinId, LocalDate.of(2026, 10, 1), LocalDate.of(2026, 10, 28), configId);

        lenient().when(pdfService.msg(anyString())).thenAnswer(call -> call.getArgument(0));
        lenient().when(pdfService.formatAmount(any())).thenAnswer(call -> call.getArgument(0, BigDecimal.class).toPlainString());
    }

    private void stubAccessAndRendering() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(pdfFormatSettingService.resolveEffectiveConfig(configId, magasinId)).thenReturn(config);
        when(strategyResolver.resolve(PdfFormat.A4)).thenReturn(reportPdfStrategy);
        when(reportPdfStrategy.generate(any(ReportPdfDocument.class), eq(magasin), eq(config))).thenReturn(pdfBytes);
    }

    private ReportPdfDocument captureRenderedDocument() {
        ArgumentCaptor<ReportPdfDocument> documentCaptor = ArgumentCaptor.forClass(ReportPdfDocument.class);
        verify(reportPdfStrategy).generate(documentCaptor.capture(), eq(magasin), eq(config));
        return documentCaptor.getValue();
    }

    @Test
    void generateOverview_should_render_the_seven_kpis_for_the_period() {
        stubAccessAndRendering();
        when(magasinReportingService.getOverview(new MagasinOverviewFilter(magasinId, reportPdfFilter.startDate(), reportPdfFilter.endDate())))
                .thenReturn(new MagasinOverviewStatsResponse(10L, new BigDecimal("500"), new BigDecimal("480"),
                        new BigDecimal("50"), 3L, 4L, 2L));

        byte[] result = reportPdfService.generateOverview(reportPdfFilter);

        assertThat(result).isEqualTo(pdfBytes);
        ReportPdfDocument document = captureRenderedDocument();
        assertThat(document.title()).isEqualTo("pdf.report.overview.title");
        assertThat(document.periodLabel()).isEqualTo("01/10/2026 - 28/10/2026");
        assertThat(document.sections()).hasSize(1);
        assertThat(document.sections().getFirst().lines())
                .extracting(ReportPdfLine::label, ReportPdfLine::value)
                .containsExactly(
                        tuple("pdf.report.kpi.chiffreAffaires", "500"),
                        tuple("pdf.report.kpi.paiementsEncaisses", "480"),
                        tuple("pdf.report.kpi.nombreVentes", "10"),
                        tuple("pdf.report.kpi.ticketMoyen", "50"),
                        tuple("pdf.report.kpi.facturesVenteImpayees", "4"),
                        tuple("pdf.report.kpi.achatsEnAttente", "3"),
                        tuple("pdf.report.kpi.facturesAchatImpayees", "2")
                );
    }

    @Test
    void generateVentes_should_render_the_five_sales_kpis() {
        stubAccessAndRendering();
        when(magasinReportingService.getVentesStats(any(MagasinOverviewFilter.class)))
                .thenReturn(new MagasinVentesStatsResponse(5L, new BigDecimal("1000"), new BigDecimal("900"),
                        new BigDecimal("200"), 1L));

        reportPdfService.generateVentes(reportPdfFilter);

        ReportPdfDocument document = captureRenderedDocument();
        assertThat(document.title()).isEqualTo("pdf.report.ventes.title");
        assertThat(document.sections().getFirst().lines()).hasSize(5);
    }

    @Test
    void generateDepenses_should_render_kpis_and_breakdown_by_category() {
        stubAccessAndRendering();
        when(depenseService.computeTotal(any(DepenseFilter.class)))
                .thenReturn(new DepenseTotalResponse(magasinId, new BigDecimal("75000"), 3L));
        when(depenseService.computeByCategory(any(DepenseFilter.class))).thenReturn(List.of(
                new DepenseParCategorieResponse(UUID.randomUUID(), "Loyer", new BigDecimal("50000"), 1L),
                new DepenseParCategorieResponse(UUID.randomUUID(), "Transport", new BigDecimal("25000"), 2L)
        ));

        reportPdfService.generateDepenses(reportPdfFilter);

        ReportPdfDocument document = captureRenderedDocument();
        assertThat(document.title()).isEqualTo("pdf.report.depenses.title");
        assertThat(document.sections()).hasSize(2);
        assertThat(document.sections().getFirst().lines())
                .extracting(ReportPdfLine::value)
                .containsExactly("75000", "3");
        assertThat(document.sections().get(1).title()).isEqualTo("pdf.report.depenses.byCategory");
        assertThat(document.sections().get(1).lines())
                .extracting(ReportPdfLine::label)
                .containsExactly("Loyer", "Transport");
    }

    @Test
    void generateDepenses_should_skip_breakdown_section_when_no_category() {
        stubAccessAndRendering();
        when(depenseService.computeTotal(any(DepenseFilter.class)))
                .thenReturn(new DepenseTotalResponse(magasinId, BigDecimal.ZERO, 0L));
        when(depenseService.computeByCategory(any(DepenseFilter.class))).thenReturn(List.of());

        reportPdfService.generateDepenses(reportPdfFilter);

        assertThat(captureRenderedDocument().sections()).hasSize(1);
    }

    @Test
    void generateOverview_should_throw_and_fetch_nothing_when_magasin_not_accessible() {
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenThrow(new ForbiddenException("magasin.notAccessible"));

        assertThatThrownBy(() -> reportPdfService.generateOverview(reportPdfFilter))
                .isInstanceOf(ForbiddenException.class);

        verifyNoInteractions(magasinReportingService, pdfFormatSettingService, strategyResolver);
    }

    @Test
    void generateDepenses_should_validate_filter() {
        stubAccessAndRendering();
        when(depenseService.computeTotal(any(DepenseFilter.class)))
                .thenReturn(new DepenseTotalResponse(magasinId, BigDecimal.ZERO, 0L));
        when(depenseService.computeByCategory(any(DepenseFilter.class))).thenReturn(List.of());

        reportPdfService.generateDepenses(reportPdfFilter);

        verify(validatorService).validate(reportPdfFilter);
    }
}
