package org.store.common.dto;

import java.util.List;

public record ExcelStockParseResult(
        List<ExcelEntreeStockRow> rows,
        List<String> errors
) {
}
