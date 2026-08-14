package org.store.achat.application.pdf.strategy;

import org.junit.jupiter.api.Test;
import org.store.common.exceptions.BadArgumentException;
import org.store.pdf.domain.enums.PdfFormat;
import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.magasin.domain.model.Magasin;
import org.store.achat.domain.model.CommandeAchat;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BonCommandePdfStrategyResolverTest {

    private static BonCommandePdfStrategy stub(PdfFormat format) {
        return new BonCommandePdfStrategy() {
            @Override public PdfFormat supports() { return format; }
            @Override public byte[] generate(CommandeAchat c, Magasin m, PdfFormatConfig cfg) { return new byte[0]; }
        };
    }

    @Test
    void resolve_should_return_matching_strategy() {
        BonCommandePdfStrategy a5 = stub(PdfFormat.A5);
        BonCommandePdfStrategy thermal = stub(PdfFormat.THERMAL_58MM);
        BonCommandePdfStrategyResolver resolver = new BonCommandePdfStrategyResolver(List.of(a5, thermal));

        assertThat(resolver.resolve(PdfFormat.A5)).isSameAs(a5);
        assertThat(resolver.resolve(PdfFormat.THERMAL_58MM)).isSameAs(thermal);
    }

    @Test
    void resolve_should_throw_bad_argument_when_format_not_registered() {
        BonCommandePdfStrategyResolver resolver = new BonCommandePdfStrategyResolver(List.of(stub(PdfFormat.A5)));

        assertThatThrownBy(() -> resolver.resolve(PdfFormat.A4))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void resolve_should_cover_all_four_formats_when_all_registered() {
        List<BonCommandePdfStrategy> all = List.of(
                stub(PdfFormat.A4),
                stub(PdfFormat.A5),
                stub(PdfFormat.THERMAL_80MM),
                stub(PdfFormat.THERMAL_58MM)
        );
        BonCommandePdfStrategyResolver resolver = new BonCommandePdfStrategyResolver(all);

        for (PdfFormat format : PdfFormat.values()) {
            assertThat(resolver.resolve(format)).isNotNull();
        }
    }
}
