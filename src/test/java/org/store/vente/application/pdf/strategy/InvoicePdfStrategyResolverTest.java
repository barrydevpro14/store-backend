package org.store.vente.application.pdf.strategy;

import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.magasin.domain.model.Magasin;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class InvoicePdfStrategyResolverTest {

    private static InvoicePdfStrategy stub(PdfFormat format) {
        return new InvoicePdfStrategy() {
            @Override public PdfFormat supports() { return format; }
            @Override public byte[] generate(FactureClient f, Magasin m, PdfFormatConfig c) { return new byte[0]; }
            @Override public byte[] generateDevis(CommandeVente c, Magasin m, PdfFormatConfig cfg) { return new byte[0]; }
        };
    }

    @Test
    void resolve_should_return_matching_strategy() {
        InvoicePdfStrategy a4 = stub(PdfFormat.A4);
        InvoicePdfStrategy thermal = stub(PdfFormat.THERMAL_80MM);
        InvoicePdfStrategyResolver resolver = new InvoicePdfStrategyResolver(List.of(a4, thermal));

        assertThat(resolver.resolve(PdfFormat.A4)).isSameAs(a4);
        assertThat(resolver.resolve(PdfFormat.THERMAL_80MM)).isSameAs(thermal);
    }

    @Test
    void resolve_should_throw_bad_argument_when_format_not_registered() {
        InvoicePdfStrategyResolver resolver = new InvoicePdfStrategyResolver(List.of(stub(PdfFormat.A4)));

        assertThatThrownBy(() -> resolver.resolve(PdfFormat.THERMAL_58MM))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void resolve_should_cover_all_four_formats_when_all_registered() {
        List<InvoicePdfStrategy> all = List.of(
                stub(PdfFormat.A4),
                stub(PdfFormat.A5),
                stub(PdfFormat.THERMAL_80MM),
                stub(PdfFormat.THERMAL_58MM)
        );
        InvoicePdfStrategyResolver resolver = new InvoicePdfStrategyResolver(all);

        for (PdfFormat format : PdfFormat.values()) {
            assertThat(resolver.resolve(format)).isNotNull();
        }
    }
}
