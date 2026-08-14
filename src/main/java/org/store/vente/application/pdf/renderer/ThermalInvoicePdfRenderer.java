package org.store.vente.application.pdf.renderer;

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
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.IPaiementVenteService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.LigneCommandeVente;

import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Génère le PDF de facture client pour les formats thermiques (80mm, 58mm).
 * Layout compact sans tableau multi-colonnes.
 */
@Service
public class ThermalInvoicePdfRenderer extends AbstractThermalPdfRenderer {

    private final IPaiementVenteService paiementVenteService;
    private final IEntrepriseSettingService entrepriseSettingService;

    public ThermalInvoicePdfRenderer(IPdfService pdf,
                                      IPaiementVenteService paiementVenteService,
                                      IEntrepriseSettingService entrepriseSettingService) {
        super(pdf);
        this.paiementVenteService = paiementVenteService;
        this.entrepriseSettingService = entrepriseSettingService;
    }

    public byte[] render(FactureClient facture, Magasin magasin, PdfFormatConfig config) {
        PdfColors colors = pdf.resolveColors(entrepriseSettingService.getMySettings().couleurPrimaire());
        LocalDateTime now = LocalDateTime.now();

        PdfHeaderContext ctx = new PdfHeaderContext(
                magasin,
                facture.getNumero(),
                now.toLocalDate(),
                now.toLocalTime(),
                facture.getDateEcheance(),
                buildClientLabel(facture),
                pdf.msg("pdf.vente.title"),
                colors,
                config
        );

        return buildDocument(config, magasin, doc -> {
            addHeader(doc, ctx);
            doc.add(Chunk.NEWLINE);
            addSeparator(doc, ctx);
            addLignesCompact(doc, facture, ctx);
            addSeparator(doc, ctx);
            addTotalsCompact(doc, facture, ctx);
        });
    }

    /* ── Client label ──────────────────────────────────────────────────── */

    private String buildClientLabel(FactureClient facture) {
        var client = facture.getCommande().getClient();
        if (client == null) return pdf.msg("pdf.vente.client.anonyme");
        String identity = joinNonBlank(" ", client.getNom(), client.getPrenom());
        return joinNonBlank(" / ", identity, client.getTelephone());
    }

    /* ── Separator ─────────────────────────────────────────────────────── */

    private void addSeparator(Document doc, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();
        Font sepFont = new Font(Font.COURIER, smallSize, Font.NORMAL, Color.GRAY);
        doc.add(new Paragraph("--------------------------------", sepFont));
    }

    /* ── Compact lines ─────────────────────────────────────────────────── */

    private void addLignesCompact(Document doc, FactureClient facture, PdfHeaderContext ctx) throws DocumentException {
        float smallSize = ctx.config().getFontSizeSmall().floatValue();

        PdfPTable table = new PdfPTable(2);
        table.setWidthPercentage(100);
        table.setWidths(new float[]{70, 30});

        for (LigneCommandeVente ligne : facture.getCommande().getLignes()) {
            addLigneCompactRow(table, ligne, smallSize);
        }

        doc.add(table);
    }

    private void addLigneCompactRow(PdfPTable table, LigneCommandeVente ligne, float smallSize) {
        var product = ligne.getProductFournisseur().getProduct();
        String symbole = product.getUniteMesure().getSymbole();
        String desc = buildProductLabel(product.getNom(), product.getReference())
                + "\n" + ligne.getQuantite() + " " + symbole
                + " × " + pdf.formatAmount(ligne.getPrixUnitaire());

        Font lineFont = new Font(Font.COURIER, smallSize, Font.NORMAL, Color.DARK_GRAY);

        PdfPCell descCell = new PdfPCell(new Phrase(desc, lineFont));
        descCell.setBorder(Rectangle.NO_BORDER);
        descCell.setPadding(3);
        table.addCell(descCell);

        PdfPCell amountCell = new PdfPCell(new Phrase(pdf.formatAmount(ligne.getMontantTotal()), lineFont));
        amountCell.setBorder(Rectangle.NO_BORDER);
        amountCell.setPadding(3);
        amountCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(amountCell);
    }

    /* ── Compact totals ────────────────────────────────────────────────── */

    private void addTotalsCompact(Document doc, FactureClient facture, PdfHeaderContext ctx) throws DocumentException {
        float normalSize = ctx.config().getFontSizeNormal().floatValue();
        PdfColors colors = ctx.colors();

        PdfPTable totals = new PdfPTable(2);
        totals.setWidthPercentage(100);
        totals.setWidths(new float[]{60, 40});

        Font labelFont = new Font(Font.COURIER, normalSize, Font.NORMAL, Color.DARK_GRAY);
        Font boldFont  = new Font(Font.COURIER, normalSize, Font.BOLD, colors.primary());

        addCompactRow(totals, pdf.msg("pdf.vente.totals.totalHt"), pdf.formatAmount(facture.getMontantTotal()), labelFont);

        for (PaiementVenteResponse p : paiementVenteService.findAllByFactureId(facture.getId())) {
            String moyen = p.moyen() != null ? p.moyen().libelle() : "—";
            addCompactRow(totals, pdf.msg("pdf.vente.totals.paiement") + " " + moyen, pdf.formatAmount(p.montant()), labelFont);
        }

        BigDecimal reste = facture.getMontantTotal().subtract(facture.getMontantPaye());
        addCompactRow(totals, pdf.msg("pdf.vente.totals.soldeRestant"), pdf.formatAmount(reste), boldFont);

        doc.add(totals);
    }

    private void addCompactRow(PdfPTable table, String label, String value, Font font) {
        PdfPCell lc = new PdfPCell(new Phrase(label, font));
        lc.setBorder(Rectangle.NO_BORDER);
        lc.setPadding(3);
        table.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(value, font));
        vc.setBorder(Rectangle.NO_BORDER);
        vc.setPadding(3);
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        table.addCell(vc);
    }
}
