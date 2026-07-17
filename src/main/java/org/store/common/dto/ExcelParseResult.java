package org.store.common.dto;

import java.util.List;

public record ExcelParseResult(
        List<ExcelProductRow> rows,
        List<String> errors
) {
}
