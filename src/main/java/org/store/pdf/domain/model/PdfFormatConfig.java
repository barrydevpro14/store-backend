package org.store.pdf.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.pdf.domain.enums.PdfFormat;

import java.math.BigDecimal;

@Getter
@Setter
@Entity
@Table(name = PdfFormatConfig.TABLE_NAME)
public class PdfFormatConfig extends AuditableEntity {

    public static final String TABLE_NAME = "pdf_format_config";

    @Column(nullable = false, length = 50, unique = true, updatable = false)
    private String code;

    @Column(nullable = false, length = 100)
    private String label;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private PdfFormat format;

    @Column(precision = 10, scale = 2)
    private BigDecimal pageWidth;

    @Column(precision = 10, scale = 2)
    private BigDecimal pageHeight;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginLeft;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginRight;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginTop;

    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal marginBottom;

    @Column(precision = 5, scale = 2)
    private BigDecimal fontSizeTitle;

    @Column(precision = 5, scale = 2)
    private BigDecimal fontSizeNormal;

    @Column(precision = 5, scale = 2)
    private BigDecimal fontSizeSmall;

    @Column(nullable = false)
    private boolean enabled = true;
}
