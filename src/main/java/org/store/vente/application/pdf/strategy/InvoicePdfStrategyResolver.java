package org.store.vente.application.pdf.strategy;

import org.springframework.stereotype.Component;
import org.store.common.exceptions.BadArgumentException;
import org.store.pdf.domain.enums.PdfFormat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class InvoicePdfStrategyResolver {

    private final Map<PdfFormat, InvoicePdfStrategy> strategies;

    public InvoicePdfStrategyResolver(List<InvoicePdfStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(InvoicePdfStrategy::supports, Function.identity()));
    }

    public InvoicePdfStrategy resolve(PdfFormat format) {
        InvoicePdfStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BadArgumentException("pdf.format.unsupported");
        }
        return strategy;
    }
}
