package org.store.pdf.application.dto;

import org.store.pdf.domain.model.PdfFormatConfig;
import org.store.pdf.domain.model.PdfFormatSetting;

import java.math.BigDecimal;
import java.util.UUID;

public record PdfFormatSettingResponse(
        UUID pdfFormatConfigId,
        String code,
        String label,
        BigDecimal pageWidth,
        BigDecimal pageHeight,
        BigDecimal marginLeft,
        BigDecimal marginRight,
        BigDecimal marginTop,
        BigDecimal marginBottom,
        BigDecimal fontSizeTitle,
        BigDecimal fontSizeNormal,
        BigDecimal fontSizeSmall,
        boolean overridden
) {

    public PdfFormatSettingResponse(PdfFormatConfig pdfFormatConfig, PdfFormatSetting pdfFormatSetting) {
        this(
                pdfFormatConfig.getId(),
                pdfFormatConfig.getCode(),
                pdfFormatConfig.getLabel(),
                pdfFormatSetting.getPageWidth(),
                pdfFormatSetting.getPageHeight(),
                pdfFormatSetting.getMarginLeft(),
                pdfFormatSetting.getMarginRight(),
                pdfFormatSetting.getMarginTop(),
                pdfFormatSetting.getMarginBottom(),
                pdfFormatSetting.getFontSizeTitle(),
                pdfFormatSetting.getFontSizeNormal(),
                pdfFormatSetting.getFontSizeSmall(),
                pdfFormatSetting.getMagasin() != null
        );
    }
}
