package org.store.common.pdf.renderer;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.store.common.pdf.PdfHeaderContext;
import org.store.common.service.IPdfService;
import org.store.common.tools.DateHelper;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.model.PdfFormatConfig;

import java.awt.*;
import java.io.ByteArrayOutputStream;

/**
 * Classe de base pour les renderers thermiques (80mm, 58mm).
 * Génère le document sur une hauteur maximale, puis le rogne à la hauteur du contenu via PdfStamper.
 * Pas de footer de page : seules l'adresse et le téléphone du magasin terminent le ticket.
 */
public abstract class AbstractThermalPdfRenderer extends AbstractPdfRenderer {

    private static final float MAX_HEIGHT = 14400f;

    protected AbstractThermalPdfRenderer(IPdfService pdf) {
        super(pdf);
    }

    /**
     * Génère le PDF thermique : contenu sur une page de MAX_HEIGHT,
     * puis CropBox recalculée pour ne montrer que la zone imprimée.
     */
    protected byte[] buildDocument(PdfFormatConfig config, Magasin magasin, DocumentConsumer consumer) {
        float w           = config.getPageWidth().floatValue();
        float marginBottom = config.getMarginBottom().floatValue();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(
                    new Rectangle(w, MAX_HEIGHT),
                    config.getMarginLeft().floatValue(),
                    config.getMarginRight().floatValue(),
                    config.getMarginTop().floatValue(),
                    marginBottom
            );
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            ThermalPositionCapture capture = new ThermalPositionCapture();
            writer.setPageEvent(capture);

            doc.open();
            consumer.accept(doc);
            addSentinel(doc, config);
            doc.close();

            return cropToContent(out.toByteArray(), w, capture.cursorY, marginBottom);
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate thermal PDF", e);
        }
    }

    private void addSentinel(Document doc, PdfFormatConfig config) throws DocumentException {
        float size = config.getFontSizeSmall().floatValue();
        Chunk sentinel = new Chunk(" ", new Font(Font.HELVETICA, size, Font.NORMAL, Color.WHITE));
        sentinel.setGenericTag("THERMAL_END");
        doc.add(new Phrase(sentinel));
    }

    private byte[] cropToContent(byte[] source, float w, float cursorY, float marginBottom) {
        float cropBottom = Math.max(0f, cursorY - marginBottom);
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfReader reader = new PdfReader(source);
            PdfDictionary page = reader.getPageN(1);
            page.put(PdfName.MEDIABOX, new PdfArray(new float[]{0f, cropBottom, w, MAX_HEIGHT}));
            page.put(PdfName.CROPBOX, new PdfArray(new float[]{0f, cropBottom, w, MAX_HEIGHT}));
            PdfStamper stamper = new PdfStamper(reader, out);
            stamper.close();
            reader.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to crop thermal PDF", e);
        }
    }

    private static final class ThermalPositionCapture extends PdfPageEventHelper {
        float cursorY = 0f;

        @Override
        public void onGenericTag(PdfWriter writer, Document document, Rectangle rect, String text) {
            if ("THERMAL_END".equals(text)) {
                cursorY = rect.getBottom();
            }
        }
    }

    /* ── Header ────────────────────────────────────────────────────────── */

    /**
     * Header compact thermique, sans libellés ni logo :
     *   1. Raison sociale
     *   2. NINEA / RCCM
     *   3. Type de document + numéro
     *   4. Date heure / tiers (client / fournisseur)
     */
    protected void addHeader(Document doc, PdfHeaderContext ctx) throws DocumentException {
        float titleSize  = ctx.config().getFontSizeTitle().floatValue();
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        float smallSize  = ctx.config().getFontSizeSmall().floatValue();
        Entreprise e     = ctx.magasin().getEntreprise();

        if (pdf.isNotBlank(e.getRaisonSociale()))
            doc.add(centeredParagraph(e.getRaisonSociale(),
                    new Font(Font.HELVETICA, titleSize, Font.BOLD, Color.BLACK)));

        String legalLine = joinNonBlank(" / ", e.getNinea(), e.getRccm());
        if (pdf.isNotBlank(legalLine))
            doc.add(centeredParagraph(legalLine, new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.BLACK)));

        doc.add(new Paragraph(joinNonBlank(" ", ctx.documentLabel(), ctx.numeroDoc()),
                new Font(Font.HELVETICA, normalSize, Font.BOLD, Color.BLACK)));

        doc.add(new Paragraph(joinNonBlank(" / ", buildDateHeure(ctx), ctx.clientLabel()),
                new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.BLACK)));
    }

    private String buildDateHeure(PdfHeaderContext ctx) {
        String date  = ctx.dateDoc()  != null ? DateHelper.formatDisplay(ctx.dateDoc()) : "—";
        String heure = ctx.heureDoc() != null ? ctx.heureDoc().format(TIME_FORMAT) : "—";
        return date + " " + heure;
    }

    /* ── Footer (adresse + téléphone du magasin) ───────────────────────── */

    protected void addFooter(Document doc, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Magasin magasin = ctx.magasin();
        Font footerFont = new Font(Font.HELVETICA, smallSize, Font.NORMAL, Color.BLACK);

        addSeparator(doc, ctx);
        if (pdf.isNotBlank(magasin.getAdresse()))
            doc.add(centeredParagraph(magasin.getAdresse(), footerFont));
        if (pdf.isNotBlank(magasin.getTelephone()))
            doc.add(centeredParagraph(magasin.getTelephone(), footerFont));
    }

    /* ── Separator ─────────────────────────────────────────────────────── */

    protected void addSeparator(Document doc, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Font sepFont = new Font(Font.COURIER, smallSize, Font.NORMAL, Color.BLACK);
        doc.add(new Paragraph("--------------------------------", sepFont));
    }
}
