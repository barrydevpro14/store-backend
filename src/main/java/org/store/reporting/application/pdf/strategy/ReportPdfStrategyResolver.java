package org.store.reporting.application.pdf.strategy;

import org.springframework.stereotype.Component;
import org.store.common.exceptions.BadArgumentException;
import org.store.pdf.domain.enums.PdfFormat;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class ReportPdfStrategyResolver {

    private final Map<PdfFormat, ReportPdfStrategy> strategies;

    public ReportPdfStrategyResolver(List<ReportPdfStrategy> strategies) {
        this.strategies = strategies.stream()
                .collect(Collectors.toMap(ReportPdfStrategy::supports, Function.identity()));
    }

    public ReportPdfStrategy resolve(PdfFormat format) {
        ReportPdfStrategy strategy = strategies.get(format);
        if (strategy == null) {
            throw new BadArgumentException("pdf.format.unsupported");
        }
        return strategy;
    }
}
