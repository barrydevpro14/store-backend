package org.store.pdf.application.dto;

import jakarta.validation.constraints.NotNull;

import java.math.BigDecimal;

public record PdfFormatSettingRequest(
        @NotNull BigDecimal pageWidth,
        BigDecimal pageHeight,
        @NotNull BigDecimal marginLeft,
        @NotNull BigDecimal marginRight,
        @NotNull BigDecimal marginTop,
        @NotNull BigDecimal marginBottom,
        @NotNull BigDecimal fontSizeTitle,
        @NotNull BigDecimal fontSizeNormal,
        @NotNull BigDecimal fontSizeSmall
) {
}
