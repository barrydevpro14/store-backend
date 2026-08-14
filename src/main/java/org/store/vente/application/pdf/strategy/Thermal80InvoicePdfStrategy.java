package org.store.vente.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.vente.application.pdf.renderer.ThermalInvoicePdfRenderer;
import org.store.vente.domain.model.FactureClient;

@Service
public class Thermal80InvoicePdfStrategy implements InvoicePdfStrategy {

    private final ThermalInvoicePdfRenderer renderer;

    public Thermal80InvoicePdfStrategy(ThermalInvoicePdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.THERMAL_80MM;
    }

    @Override
    public byte[] generate(FactureClient facture, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(facture, magasin, config);
    }
}
