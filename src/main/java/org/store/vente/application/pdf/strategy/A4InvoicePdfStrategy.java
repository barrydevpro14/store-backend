package org.store.vente.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.vente.application.pdf.renderer.StandardInvoicePdfRenderer;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;

@Service
public class A4InvoicePdfStrategy implements InvoicePdfStrategy {

    private final StandardInvoicePdfRenderer renderer;

    public A4InvoicePdfStrategy(StandardInvoicePdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A4;
    }

    @Override
    public byte[] generate(FactureClient facture, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(facture, magasin, config);
    }

    @Override
    public byte[] generateDevis(CommandeVente commande, Magasin magasin, PdfFormatConfig config) {
        return renderer.renderDevis(commande, magasin, config);
    }
}
