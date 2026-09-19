package org.store.pdf.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.magasin.domain.model.Magasin;

import java.math.BigDecimal;

/**
 * Données de paramétrage PDF (largeur, marges, tailles de police) pour un format donné.
 * `magasin = null` = valeurs globales par défaut ; `magasin` renseigné = surcharge propre à ce magasin.
 */
@Getter
@Setter
@Entity
@Table(name = PdfFormatSetting.TABLE_NAME)
public class PdfFormatSetting extends AuditableEntity {

    public static final String TABLE_NAME = "pdf_format_setting";

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "pdf_format_config_id", nullable = false, updatable = false)
    private PdfFormatConfig pdfFormatConfig;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "magasin_id", updatable = false)
    private Magasin magasin;

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
}
