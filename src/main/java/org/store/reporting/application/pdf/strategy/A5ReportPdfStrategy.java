package org.store.reporting.application.pdf.strategy;

import org.springframework.stereotype.Service;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.ReportPdfDocument;
import org.store.reporting.application.pdf.renderer.StandardReportPdfRenderer;

@Service
public class A5ReportPdfStrategy implements ReportPdfStrategy {

    private final StandardReportPdfRenderer renderer;

    public A5ReportPdfStrategy(StandardReportPdfRenderer renderer) {
        this.renderer = renderer;
    }

    @Override
    public PdfFormat supports() {
        return PdfFormat.A5;
    }

    @Override
    public byte[] generate(ReportPdfDocument reportPdfDocument, Magasin magasin, PdfFormatConfig config) {
        return renderer.render(reportPdfDocument, magasin, config);
    }
}
