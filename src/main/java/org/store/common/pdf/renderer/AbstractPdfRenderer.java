package org.store.common.pdf.renderer;

import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Paragraph;
import org.store.common.service.IPdfService;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Quality;

import java.time.format.DateTimeFormatter;

/**
 * Utilitaires communs partagés par AbstractStandardPdfRenderer et AbstractThermalPdfRenderer.
 * Ne contient aucune logique de layout.
 */
public abstract class AbstractPdfRenderer {

    @FunctionalInterface
    protected interface DocumentConsumer {
        void accept(Document doc) throws Exception;
    }

    protected static final DateTimeFormatter TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm");

    protected final IPdfService pdf;

    protected AbstractPdfRenderer(IPdfService pdf) {
        this.pdf = pdf;
    }

    protected Paragraph centeredParagraph(String text, Font font) {
        Paragraph p = new Paragraph(text, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    protected String joinNonBlank(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!pdf.isNotBlank(part)) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(part);
        }
        return sb.toString();
    }

    protected String buildProductLabel(String nom, String ref) {
        return pdf.isNotBlank(ref) ? nom + " (" + ref + ")" : pdf.nullToEmpty(nom);
    }

    protected String buildCategoryQualityLabel(CategoryProduct category, Quality quality) {
        String categoryLabel = category != null && pdf.isNotBlank(category.getLibelle()) ? category.getLibelle() : null;
        String qualityLabel  = quality  != null && pdf.isNotBlank(quality.getLibelle())  ? quality.getLibelle()  : null;

        if (categoryLabel != null && qualityLabel != null) return categoryLabel + " / " + qualityLabel;
        if (categoryLabel != null) return categoryLabel;
        if (qualityLabel  != null) return qualityLabel;
        return "—";
    }
}
