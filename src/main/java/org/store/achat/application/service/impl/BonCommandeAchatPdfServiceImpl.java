package org.store.achat.application.service.impl;

import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.achat.application.service.IBonCommandeAchatPdfService;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.common.service.IPdfService;
import org.store.common.tools.OwnershipHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.produit.domain.model.CategoryProduct;
import org.store.produit.domain.model.Quality;
import org.store.security.application.service.ICurrentUserService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.util.UUID;

/**
 * Generates a PDF purchase order for an achat using OpenPDF.
 * Layout: store header / order metadata / supplier / lines table (no prix vente) / total.
 */
@Service
@Transactional(readOnly = true)
public class BonCommandeAchatPdfServiceImpl implements IBonCommandeAchatPdfService {

    private final CommandeAchatDomainService commandeAchatDomainService;
    private final ICurrentUserService currentUserService;
    private final IPdfService pdf;

    public BonCommandeAchatPdfServiceImpl(CommandeAchatDomainService commandeAchatDomainService,
                                           ICurrentUserService currentUserService,
                                           IPdfService pdf) {
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.currentUserService = currentUserService;
        this.pdf = pdf;
    }

    @Override
    public byte[] generate(UUID commandeId) {
        CommandeAchat commande = commandeAchatDomainService.findById(commandeId);

        OwnershipHelper.ensureOwnership(
                commande,
                commande.getMagasin().getEntreprise().getId(),
                currentUserService.getCurrent().entrepriseId(),
                "commandeAchat.notOwned"
        );

        Magasin magasin = commande.getMagasin();

        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Document doc = new Document(PageSize.A4, 40, 40, 40, 120);
            PdfWriter writer = PdfWriter.getInstance(doc, out);
            pdf.configureFooter(writer, magasin);
            doc.open();

            addHeader(doc, magasin, commande);
            doc.add(Chunk.NEWLINE);
            addSupplier(doc, commande);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, commande);
            doc.add(Chunk.NEWLINE);
            addTotal(doc, commande);

            doc.close();
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate purchase order PDF", e);
        }
    }

    /* ── Header ────────────────────────────────────────────────────────── */

    private void addHeader(Document doc, Magasin magasin, CommandeAchat commande) throws DocumentException {
        PdfPTable header = new PdfPTable(2);
        header.setWidthPercentage(100);
        header.setWidths(new float[]{55, 45});

        header.addCell(pdf.buildStoreCell(magasin));

        PdfPCell orderCell = new PdfPCell();
        orderCell.setBorder(Rectangle.NO_BORDER);
        orderCell.setBackgroundColor(IPdfService.LIGHT_BG);
        orderCell.setPadding(12);
        orderCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, IPdfService.PRIMARY);
        Font refFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
        Font dateFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT);

        orderCell.addElement(new Paragraph(pdf.msg("pdf.achat.title"), titleFont));
        orderCell.addElement(new Paragraph(commande.getReference(), refFont));
        if (commande.getDate() != null)
            orderCell.addElement(new Paragraph(pdf.msg("pdf.label.date") + " : " + commande.getDate().format(IPdfService.DATE_FMT), dateFont));

        header.addCell(orderCell);
        doc.add(header);
    }

    /* ── Supplier ──────────────────────────────────────────────────────── */

    private void addSupplier(Document doc, CommandeAchat commande) throws DocumentException {
        if (commande.getFournisseur() == null) return;

        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font infoFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, IPdfService.GRAY_TEXT);
        var fournisseur = commande.getFournisseur();

        PdfPCell cell = pdf.sectionCell(pdf.msg("pdf.achat.section.fournisseur"));

        String identityLine = joinNonBlank(" / ",
                joinNonBlank(" ", fournisseur.getNom(), fournisseur.getPrenom()),
                fournisseur.getReference());
        String contactLine = joinNonBlank(" / ",
                fournisseur.getTelephone(), fournisseur.getEmail(), fournisseur.getAdresse());

        if (pdf.isNotBlank(identityLine))
            cell.addElement(new Paragraph(identityLine, valueFont));
        if (pdf.isNotBlank(contactLine))
            cell.addElement(new Paragraph(contactLine, infoFont));

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
        doc.add(table);
    }

    private String joinNonBlank(String separator, String... parts) {
        StringBuilder sb = new StringBuilder();
        for (String part : parts) {
            if (!pdf.isNotBlank(part)) continue;
            if (sb.length() > 0) sb.append(separator);
            sb.append(part);
        }
        return sb.toString();
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, CommandeAchat commande) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{35, 30, 12, 23});
        table.setWidthPercentage(100);

        Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        String[] headers = {
            pdf.msg("pdf.achat.table.produit"),
            pdf.msg("pdf.table.categorieQualite"),
            pdf.msg("pdf.achat.table.quantite"),
            pdf.msg("pdf.achat.table.prixAchat")
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], headFont));
            cell.setBackgroundColor(IPdfService.PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(i <= 1 ? Element.ALIGN_LEFT : Element.ALIGN_RIGHT);
            table.addCell(cell);
        }

        Font lineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        boolean alt   = false;

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            var product = ligne.getProductFournisseur().getProduct();
            var quality = ligne.getProductFournisseur().getQuality();

            table.addCell(pdf.textCell(buildProductLabel(product.getNom(), product.getReference()), lineFont, bg));
            table.addCell(pdf.textCell(buildCategoryQualityLabel(product.getCategoryProduct(), quality), lineFont, bg));
            table.addCell(pdf.numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixAchat()), lineFont, bg));
        }

        doc.add(table);
    }

    private String buildProductLabel(String nom, String ref) {
        return pdf.isNotBlank(ref) ? nom + "(" + ref + ")" : pdf.nullToEmpty(nom);
    }

    private String buildCategoryQualityLabel(CategoryProduct category, Quality quality) {
        String categoryLabel = category != null && pdf.isNotBlank(category.getLibelle()) ? category.getLibelle() : null;
        String qualityLabel  = quality != null && pdf.isNotBlank(quality.getLibelle()) ? quality.getLibelle() : null;

        if (categoryLabel != null && qualityLabel != null) return categoryLabel + " / " + qualityLabel;
        if (categoryLabel != null) return categoryLabel;
        if (qualityLabel != null) return qualityLabel;
        return "—";
    }

    /* ── Total ─────────────────────────────────────────────────────────── */

    private void addTotal(Document doc, CommandeAchat commande) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD, IPdfService.PRIMARY);
        pdf.addTotalRow(totals, pdf.msg("pdf.achat.total"), pdf.formatAmount(commande.getMontantTotal()), boldFont, boldFont, IPdfService.LIGHT_BG);

        doc.add(totals);
    }
}
