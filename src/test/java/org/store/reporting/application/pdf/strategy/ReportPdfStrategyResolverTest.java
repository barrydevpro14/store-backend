package org.store.reporting.application.pdf.strategy;

import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.magasin.domain.model.Magasin;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.reporting.application.dto.ReportPdfDocument;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ReportPdfStrategyResolverTest {

    private static ReportPdfStrategy stub(PdfFormat format) {
        return new ReportPdfStrategy() {
            @Override public PdfFormat supports() { return format; }
            @Override public byte[] generate(ReportPdfDocument reportPdfDocument, Magasin magasin, PdfFormatConfig config) { return new byte[0]; }
        };
    }

    @Test
    void resolve_should_return_matching_strategy() {
        ReportPdfStrategy a4 = stub(PdfFormat.A4);
        ReportPdfStrategy thermal = stub(PdfFormat.THERMAL_58MM);
        ReportPdfStrategyResolver resolver = new ReportPdfStrategyResolver(List.of(a4, thermal));

        assertThat(resolver.resolve(PdfFormat.THERMAL_58MM)).isSameAs(thermal);
    }

    @Test
    void resolve_should_throw_when_format_not_supported() {
        ReportPdfStrategyResolver resolver = new ReportPdfStrategyResolver(List.of(stub(PdfFormat.A4)));

        assertThatThrownBy(() -> resolver.resolve(PdfFormat.A5))
                .isInstanceOf(BadArgumentException.class);
    }
}
