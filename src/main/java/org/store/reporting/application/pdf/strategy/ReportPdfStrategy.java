package org.store.reporting.application.pdf.strategy;

import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.ReportPdfDocument;

public interface ReportPdfStrategy {

    PdfFormat supports();

    byte[] generate(ReportPdfDocument reportPdfDocument, Magasin magasin, PdfFormatConfig config);
}
