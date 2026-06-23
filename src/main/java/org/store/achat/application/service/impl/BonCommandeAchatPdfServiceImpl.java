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
import org.store.common.tools.OwnershipHelper;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.service.ICurrentUserService;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

/**
 * Generates a PDF purchase order for an achat using OpenPDF.
 * Layout: store header / order metadata / supplier / lines table (no prix vente) / total.
 */
@Service
@Transactional(readOnly = true)
public class BonCommandeAchatPdfServiceImpl implements IBonCommandeAchatPdfService {

    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final Color PRIMARY   = new Color(37, 99, 235);
    private static final Color LIGHT_BG  = new Color(239, 246, 255);
    private static final Color GRAY_TEXT = new Color(107, 114, 128);
    private static final Color BORDER    = new Color(229, 231, 235);

    private final CommandeAchatDomainService commandeAchatDomainService;
    private final ICurrentUserService currentUserService;

    public BonCommandeAchatPdfServiceImpl(CommandeAchatDomainService commandeAchatDomainService,
                                           ICurrentUserService currentUserService) {
        this.commandeAchatDomainService = commandeAchatDomainService;
        this.currentUserService = currentUserService;
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
            addFooter(doc, magasin);

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

        PdfPCell storeCell = new PdfPCell();
        storeCell.setBorder(Rectangle.NO_BORDER);
        storeCell.setPadding(8);

        Font nameFont = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
        Font infoFont = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);

        storeCell.addElement(new Paragraph(magasin.getNom(), nameFont));
        if (isNotBlank(magasin.getAdresse()))
            storeCell.addElement(new Paragraph(magasin.getAdresse(), infoFont));
        if (isNotBlank(magasin.getTelephone()))
            storeCell.addElement(new Paragraph(magasin.getTelephone(), infoFont));

        header.addCell(storeCell);

        PdfPCell orderCell = new PdfPCell();
        orderCell.setBorder(Rectangle.NO_BORDER);
        orderCell.setBackgroundColor(LIGHT_BG);
        orderCell.setPadding(12);
        orderCell.setHorizontalAlignment(Element.ALIGN_RIGHT);

        Font titleFont = new Font(Font.HELVETICA, 16, Font.BOLD, PRIMARY);
        Font refFont   = new Font(Font.HELVETICA, 11, Font.BOLD, Color.DARK_GRAY);
        Font dateFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);

        orderCell.addElement(new Paragraph("BON DE COMMANDE", titleFont));
        orderCell.addElement(new Paragraph(commande.getReference(), refFont));
        if (commande.getDate() != null)
            orderCell.addElement(new Paragraph("Date : " + commande.getDate().format(DATE_FMT), dateFont));

        header.addCell(orderCell);
        doc.add(header);
    }

    /* ── Supplier ──────────────────────────────────────────────────────── */

    private void addSupplier(Document doc, CommandeAchat commande) throws DocumentException {
        if (commande.getFournisseur() == null) return;

        Font valueFont = new Font(Font.HELVETICA, 10, Font.NORMAL, Color.DARK_GRAY);
        Font infoFont  = new Font(Font.HELVETICA, 9, Font.NORMAL, GRAY_TEXT);
        var fournisseur = commande.getFournisseur();

        PdfPCell cell = sectionCell("FOURNISSEUR");
        cell.addElement(new Paragraph(nullToEmpty(fournisseur.getNom()), valueFont));
        if (isNotBlank(fournisseur.getTelephone()))
            cell.addElement(new Paragraph(fournisseur.getTelephone(), infoFont));
        if (isNotBlank(fournisseur.getAdresse()))
            cell.addElement(new Paragraph(fournisseur.getAdresse(), infoFont));
        if (isNotBlank(fournisseur.getReference()))
            cell.addElement(new Paragraph("Réf. : " + fournisseur.getReference(), infoFont));

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
        String[] headers = {"Produit", "Réf.", "Qté", "Prix achat"};
        for (String h : headers) {
            PdfPCell cell = new PdfPCell(new Phrase(h, headFont));
            cell.setBackgroundColor(PRIMARY);
            cell.setPadding(7);
            cell.setBorder(Rectangle.NO_BORDER);
            boolean isRight = h.equals("Qté") || h.equals("Prix achat");
            cell.setHorizontalAlignment(isRight ? Element.ALIGN_RIGHT : Element.ALIGN_LEFT);
            table.addCell(cell);
        }

        Font lineFont = new Font(Font.HELVETICA, 9, Font.NORMAL, Color.DARK_GRAY);
        Font refFont  = new Font(Font.HELVETICA, 8, Font.NORMAL, GRAY_TEXT);
        boolean alt   = false;

        for (LigneCommandeAchat ligne : commande.getLignes()) {
            Color bg = alt ? new Color(249, 250, 251) : Color.WHITE;
            alt = !alt;

            PdfPCell nameCell = new PdfPCell();
            nameCell.setBackgroundColor(bg);
            nameCell.setBorderColor(BORDER);
            nameCell.setPadding(6);
            String nom = ligne.getProductFournisseur().getProduct().getNom();
            String ref = ligne.getProductFournisseur().getProduct().getReference();
            nameCell.addElement(new Paragraph(nom, lineFont));
            if (isNotBlank(ref))
                nameCell.addElement(new Paragraph(ref, refFont));
            table.addCell(nameCell);

            table.addCell(textCell(nullToEmpty(ref), refFont, bg));
            table.addCell(numCell(String.valueOf(ligne.getQuantite()), lineFont, bg));
            table.addCell(numCell(formatAmount(ligne.getPrixAchat()), lineFont, bg));
        }

        doc.add(table);
    }

    /* ── Total ─────────────────────────────────────────────────────────── */

    private void addTotal(Document doc, CommandeAchat commande) throws DocumentException {
        PdfPTable totals = new PdfPTable(new float[]{65, 35});
        totals.setWidthPercentage(100);

        Font boldFont = new Font(Font.HELVETICA, 11, Font.BOLD, PRIMARY);

        PdfPCell lc = new PdfPCell(new Phrase("Total", boldFont));
        lc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lc.setBorderColor(BORDER);
        lc.setBackgroundColor(LIGHT_BG);
        lc.setPadding(8);
        totals.addCell(lc);

        PdfPCell vc = new PdfPCell(new Phrase(formatAmount(commande.getMontantTotal()), boldFont));
        vc.setHorizontalAlignment(Element.ALIGN_RIGHT);
        vc.setBorderColor(BORDER);
        vc.setBackgroundColor(LIGHT_BG);
        vc.setPadding(8);
        totals.addCell(vc);

        doc.add(totals);
    }

    /* ── Footer ────────────────────────────────────────────────────────── */

    private void addFooter(Document doc, Magasin magasin) throws DocumentException {
        doc.add(Chunk.NEWLINE);
        Font footerFont = new Font(Font.HELVETICA, 8, Font.ITALIC, GRAY_TEXT);
        String text = "Document généré par Store ERP";
        if (isNotBlank(magasin.getNom())) text = magasin.getNom() + " – " + text;
        Paragraph footer = new Paragraph(text, footerFont);
        footer.setAlignment(Element.ALIGN_CENTER);
        doc.add(footer);
    }

    /* ── Helpers ───────────────────────────────────────────────────────── */

    private PdfPCell sectionCell(String title) {
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOX);
        cell.setBorderColor(BORDER);
        cell.setPadding(10);
        cell.addElement(new Paragraph(title, new Font(Font.HELVETICA, 8, Font.BOLD, GRAY_TEXT)));
        cell.addElement(Chunk.NEWLINE);
        return cell;
    }

    private PdfPCell textCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    private PdfPCell numCell(String text, Font font, Color bg) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        cell.setBorderColor(BORDER);
        cell.setBackgroundColor(bg);
        cell.setPadding(6);
        return cell;
    }

    private String formatAmount(BigDecimal amount) {
        if (amount == null) return "0";
        return String.format("%,.0f", amount);
    }

    private boolean isNotBlank(String s) { return s != null && !s.isBlank(); }
    private String nullToEmpty(String s)  { return s == null ? "" : s; }
}
