package org.store.reporting.application.pdf.renderer;

import com.lowagie.text.Phrase;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfReader;
import com.lowagie.text.pdf.parser.PdfTextExtractor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.store.common.dto.PdfColors;
import org.store.common.service.IPdfService;
import org.store.entreprise.application.dto.EntrepriseSettingResponse;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.dto.ReportPdfLine;
import org.store.reporting.application.dto.ReportPdfSection;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ReportPdfRendererTest {

    private IPdfService pdfService;
    private IEntrepriseSettingService entrepriseSettingService;

    @BeforeEach
    void setUp() {
        pdfService = mock(IPdfService.class);
        when(pdfService.msg(anyString())).thenAnswer(call -> call.getArgument(0));
        when(pdfService.isNotBlank(any())).thenAnswer(call -> {
            String value = call.getArgument(0);
            return value != null && !value.isBlank();
        });
        when(pdfService.resolveColors(any())).thenReturn(PdfColors.defaults());
        when(pdfService.textCell(any(), any(), any())).thenAnswer(call -> new PdfPCell(
                new Phrase(call.getArgument(0, String.class), call.getArgument(1))));
        when(pdfService.numCell(any(), any(), any())).thenAnswer(call -> new PdfPCell(
                new Phrase(call.getArgument(0, String.class), call.getArgument(1))));

        entrepriseSettingService = mock(IEntrepriseSettingService.class);
        when(entrepriseSettingService.getMySettings()).thenReturn(new EntrepriseSettingResponse((String) null));
    }

    @Test
    void thermal_render_should_print_title_period_kpis_breakdown_and_magasin_footer() throws Exception {
        ThermalReportPdfRenderer renderer = new ThermalReportPdfRenderer(pdfService, entrepriseSettingService);

        String text = extractText(renderer.render(sampleDocument(), sampleMagasin(), sampleConfig(new BigDecimal("226"), BigDecimal.ZERO)));

        assertThat(text)
                .contains("BARRY QUINCAILLERIE GENERALE")
                .contains("pdf.label.periode 01/10/2026 - 28/10/2026")
                .doesNotContain("RAPPORT DES DÉPENSES")
                .contains("Total des dépenses")
                .contains("75 000")
                .contains("Répartition par catégorie")
                .contains("Loyer")
                .contains("+221773690065");
    }

    @Test
    void standard_render_should_print_title_period_and_every_section() throws Exception {
        StandardReportPdfRenderer renderer = new StandardReportPdfRenderer(pdfService, entrepriseSettingService);

        String text = extractText(renderer.render(sampleDocument(), sampleMagasin(), sampleConfig(new BigDecimal("595"), new BigDecimal("842"))));

        assertThat(text)
                .contains("RAPPORT DES DÉPENSES")
                .contains("pdf.label.periode")
                .contains("01/10/2026 - 28/10/2026")
                .contains("Indicateur")
                .contains("Nombre de dépenses")
                .contains("Répartition par catégorie")
                .contains("Transport");
    }

    private ReportPdfDocument sampleDocument() {
        ReportPdfSection kpis = new ReportPdfSection(null, "Indicateur", "Valeur", List.of(
                new ReportPdfLine("Total des dépenses", "75 000"),
                new ReportPdfLine("Nombre de dépenses", "3")
        ));
        ReportPdfSection categories = new ReportPdfSection("Répartition par catégorie", "Catégorie", "Montant", List.of(
                new ReportPdfLine("Loyer", "50 000"),
                new ReportPdfLine("Transport", "25 000")
        ));
        return new ReportPdfDocument("RAPPORT DES DÉPENSES", "01/10/2026 - 28/10/2026", List.of(kpis, categories));
    }

    private Magasin sampleMagasin() {
        Entreprise entreprise = new Entreprise();
        entreprise.setRaisonSociale("BARRY QUINCAILLERIE GENERALE");

        Magasin magasin = new Magasin();
        magasin.setNom("Boutique Centre");
        magasin.setAdresse("KEUR MASSAR NORD");
        magasin.setTelephone("+221773690065");
        magasin.setEntreprise(entreprise);
        return magasin;
    }

    private PdfFormatConfig sampleConfig(BigDecimal pageWidth, BigDecimal pageHeight) {
        PdfFormatConfig config = new PdfFormatConfig();
        config.setPageWidth(pageWidth);
        config.setPageHeight(pageHeight);
        config.setMarginLeft(new BigDecimal("8"));
        config.setMarginRight(new BigDecimal("8"));
        config.setMarginTop(new BigDecimal("8"));
        config.setMarginBottom(new BigDecimal("8"));
        config.setFontSizeTitle(new BigDecimal("11"));
        config.setFontSizeNormal(new BigDecimal("9"));
        config.setFontSizeSmall(new BigDecimal("8"));
        return config;
    }

    private String extractText(byte[] pdfBytes) throws Exception {
        PdfReader reader = new PdfReader(pdfBytes);
        try {
            return new PdfTextExtractor(reader).getTextFromPage(1);
        } finally {
            reader.close();
        }
    }
}
