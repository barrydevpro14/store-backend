package org.store.achat.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.achat.application.pdf.renderer.ThermalBonCommandePdfRenderer;
import org.store.achat.domain.model.CommandeAchat;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;

@Service
public class Thermal58BonCommandePdfStrategy implements BonCommandePdfStrategy {

    private final ThermalBonCommandePdfRenderer renderer;

    public Thermal58BonCommandePdfStrategy(ThermalBonCommandePdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.THERMAL_58MM;
    }

    @Override
    public byte[] generate(CommandeAchat commande, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(commande, magasin, config);
    }
}
