package org.store.achat.application.pdf.strategy;

import org.springframework.stereotype.Component;
import org.store.common.exceptions.BadArgumentException;
import org.store.pdf.domain.enums.PdfFormat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class BonCommandePdfStrategyResolver {

    private final Map<PdfFormat, BonCommandePdfStrategy> strategies;

    public BonCommandePdfStrategyResolver(List<BonCommandePdfStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(BonCommandePdfStrategy::supports, Function.identity()));
    }

    public BonCommandePdfStrategy resolve(PdfFormat format) {
        BonCommandePdfStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BadArgumentException("pdf.format.unsupported");
        }
        return strategy;
    }
}
