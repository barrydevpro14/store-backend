package org.store.reporting.application.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import org.springframework.stereotype.Service;
import org.store.common.dto.PdfColors;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.pdf.renderer.AbstractStandardPdfRenderer;
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
 * Génère le PDF d'un rapport (KPI + sections optionnelles) pour les formats A4 et A5.
 * Une section = un tableau libellé | valeur.
 */
@Service
public class StandardReportPdfRenderer extends AbstractStandardPdfRenderer {

    private final IEntrepriseSettingService entrepriseSettingService;

    public StandardReportPdfRenderer(IPdfService pdf, IEntrepriseSettingService entrepriseSettingService) {
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
                null,
                reportPdfDocument.title(),
                colors,
                config
        );

        return buildDocument(config, magasin, doc -> {
            addReportHeader(doc, ctx);
            for (ReportPdfSection section : reportPdfDocument.sections()) {
                doc.add(Chunk.NEWLINE);
                addSection(doc, section, ctx);
            }
        });
    }

    /* ── Section (titre + tableau libellé | valeur) ────────────────────── */

    private void addSection(Document doc, ReportPdfSection section, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        float smallSize  = ctx.config().getFontSizeSmall().floatValue();

        if (pdf.isNotBlank(section.title())) {
            doc.add(new Paragraph(section.title(), new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY)));
            doc.add(Chunk.NEWLINE);
        }

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{65, 35});

        Font headFont = new Font(Font.HELVETICA, smallSize, Font.BOLD, Color.WHITE);
        table.addCell(buildHeadCell(section.labelHeader(), headFont, ctx, Element.ALIGN_LEFT));
        table.addCell(buildHeadCell(section.valueHeader(), headFont, ctx, Element.ALIGN_RIGHT));

        Font lineFont = new Font(Font.HELVETICA, normalSize, Font.NORMAL, Color.DARK_GRAY);
        Font valueFont = new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.DARK_GRAY);
        boolean alt = false;

        for (ReportPdfLine line : section.lines()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;
            table.addCell(pdf.textCell(line.label(), lineFont, bg));
            table.addCell(pdf.numCell(line.value(), valueFont, bg));
        }

        doc.add(table);
    }

    private PdfPCell buildHeadCell(String text, Font headFont, PdfHeaderContext ctx, int alignment) {
        PdfPCell cell = new PdfPCell(new Phrase(text, headFont));
        cell.setBackgroundColor(ctx.colors().primary());
        cell.setPadding(7);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alignment);
        return cell;
    }
}
