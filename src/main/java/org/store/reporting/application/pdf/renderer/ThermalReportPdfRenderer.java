package org.store.reporting.application.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.store.common.dto.PdfColors;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.pdf.renderer.AbstractThermalPdfRenderer;
import org.store.common.service.IPdfService;
import org.store.entreprise.application.service.IEntrepriseSettingService;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.dto.ReportPdfLine;
import org.store.reporting.application.dto.ReportPdfSection;

import java.awt.*;
import java.time.LocalDateTime;

/**
 * Génère le PDF d'un rapport pour les formats thermiques (80mm, 58mm).
 * Même header compact que le ticket : PÉRIODE + dates, date / magasin, puis lignes libellé ……… valeur.
 */
@Service
public class ThermalReportPdfRenderer extends AbstractThermalPdfRenderer {

    private final IEntrepriseSettingService entrepriseSettingService;

    public ThermalReportPdfRenderer(IPdfService pdf, IEntrepriseSettingService entrepriseSettingService) {
        super(pdf);
        this.entrepriseSettingService = entrepriseSettingService;
    }

    public byte[] render(ReportPdfDocument reportPdfDocument, Magasin magasin, PdfFormatConfig config) {
        PdfColors colors = pdf.resolveColors(entrepriseSettingService.getMySettings().couleurPrimaire());
        LocalDateTime now = LocalDateTime.now();

        PdfHeaderContext ctx = new PdfHeaderContext(
                magasin,
                reportPdfDocument.periodLabel(),
                now.toLocalDate(),
                now.toLocalTime(),
                null,
                magasin.getNom(),
                pdf.msg("pdf.label.periode"),
                colors,
                config
        );

        return buildDocument(config, magasin, doc -> {
            addHeader(doc, ctx);
            for (ReportPdfSection section : reportPdfDocument.sections()) {
                addSeparator(doc, ctx);
                addSection(doc, section, ctx);
            }
            addFooter(doc, ctx);
        });
    }

    /* ── Section (titre + lignes libellé / valeur) ─────────────────────── */

    private void addSection(Document doc, ReportPdfSection section, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();

        if (pdf.isNotBlank(section.title())) {
            doc.add(new Paragraph(section.title(), new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.BLACK)));
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{65, 35});

        Font lineFont = new Font(Font.HELVETICA, normalSize, Font.NORMAL, Color.BLACK);

        for (ReportPdfLine line : section.lines()) {
            table.addCell(buildLineCell(line.label(), lineFont, Element.ALIGN_LEFT));
            table.addCell(buildLineCell(line.value(), lineFont, Element.ALIGN_RIGHT));
        }

        doc.add(table);
    }

    private PdfPCell buildLineCell(String text, Font font, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(3);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
