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
            Document doc = new Document(PageSize.A4, 40, 40, 40, 40);
            PdfWriter.getInstance(doc, out);
            doc.open();

            addHeader(doc, magasin, commande);
            doc.add(Chunk.NEWLINE);
            addSupplier(doc, commande);
            doc.add(Chunk.NEWLINE);
            addLinesTable(doc, commande);
            doc.add(Chunk.NEWLINE);
            addTotal(doc, commande);
            pdf.addFooter(doc, magasin);

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
        cell.addElement(new Paragraph(pdf.nullToEmpty(fournisseur.getNom()), valueFont));
        if (pdf.isNotBlank(fournisseur.getTelephone()))
            cell.addElement(new Paragraph(fournisseur.getTelephone(), infoFont));
        if (pdf.isNotBlank(fournisseur.getAdresse()))
            cell.addElement(new Paragraph(fournisseur.getAdresse(), infoFont));
        if (pdf.isNotBlank(fournisseur.getReference()))
            cell.addElement(new Paragraph(pdf.msg("pdf.label.ref") + " : " + fournisseur.getReference(), infoFont));

        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(50);
        table.setHorizontalAlignment(Element.ALIGN_LEFT);
        table.addCell(cell);
        doc.add(table);
    }

    /* ── Lines table ───────────────────────────────────────────────────── */

    private void addLinesTable(Document doc, CommandeAchat commande) throws DocumentException {
        PdfPTable table = new PdfPTable(new float[]{40, 20, 15, 25});
        table.setWidthPercentage(100);

        Font headFont = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
        String[] headers = {
            pdf.msg("pdf.achat.table.produit"),
            pdf.msg("pdf.achat.table.reference"),
            pdf.msg("pdf.achat.table.quantite"),
            pdf.msg("pdf.achat.table.prixAchat")
        };
        for (int i = 0; i < headers.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(headers[i], headFont));
            cell.setBackgroundColor(IPdfService.PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            cell.setHorizontalAlignment(i >= 2 ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        Font lineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font refFont  = new Font(Font.HELVETICA, 8, Font.NORMAL, IPdfService.GRAY_TEXT);
        boolean alt   = false;

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(bg);
            nameCell.setBorderColor(IPdfService.BORDER);
            nameCell.setPadding(6);
            var product = ligne.getProductFournisseur().getProduct();
            String nom = product.getNom();
            String ref = product.getReference();
            nameCell.addElement(new Paragraph(nom, lineFont));
            if (pdf.isNotBlank(ref))
                nameCell.addElement(new Paragraph(ref, refFont));
            var category = product.getCategoryProduct();
            if (category != null && pdf.isNotBlank(category.getLibelle()))
                nameCell.addElement(new Paragraph(pdf.msg("pdf.label.category") + " : " + category.getLibelle(), refFont));
            var quality = ligne.getProductFournisseur().getQuality();
            if (quality != null && pdf.isNotBlank(quality.getLibelle()))
                nameCell.addElement(new Paragraph(pdf.msg("pdf.label.qualite") + " : " + quality.getLibelle(), refFont));
            table.addCell(nameCell);

            table.addCell(pdf.textCell(pdf.nullToEmpty(ref), refFont, bg));
            table.addCell(pdf.numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(pdf.numCell(pdf.formatAmount(ligne.getPrixAchat()), lineFont, bg));
        }

        doc.add(table);
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
