package org.store.vente.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.vente.application.pdf.renderer.StandardInvoicePdfRenderer;
import org.store.vente.domain.model.FactureClient;

@Service
public class A5InvoicePdfStrategy implements InvoicePdfStrategy {

    private final StandardInvoicePdfRenderer renderer;

    public A5InvoicePdfStrategy(StandardInvoicePdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A5;
    }

    @Override
    public byte[] generate(FactureClient facture, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(facture, magasin, config);
    }
}
