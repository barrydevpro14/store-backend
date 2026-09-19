package org.store.pdf.domain.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import org.store.common.base.AuditableEntity;
import org.store.pdf.domain.enums.PdfFormat;

import java.math.BigDecimal;

/**
 * Catalogue des formats PDF (identité : code, libellé, type de rendu).
 * Les valeurs de paramétrage (largeur, marges, tailles de police) sont dissociées
 * dans {@link PdfFormatSetting} — les champs ci-dessous sont transients, remplis
 * à la résolution effective (globale ou surchargée par magasin).
 */
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

    @Column(nullable = false)
    private boolean enabled = true;

    @Transient
    private BigDecimal pageWidth;

    @Transient
    private BigDecimal pageHeight;

    @Transient
    private BigDecimal marginLeft;

    @Transient
    private BigDecimal marginRight;

    @Transient
    private BigDecimal marginTop;

    @Transient
    private BigDecimal marginBottom;

    @Transient
    private BigDecimal fontSizeTitle;

    @Transient
    private BigDecimal fontSizeNormal;

    @Transient
    private BigDecimal fontSizeSmall;

    /** Copies the effective numeric parametrage values from the resolved setting onto this transient view. */
    public void applySetting(PdfFormatSetting setting) {
        this.pageWidth = setting.getPageWidth();
        this.pageHeight = setting.getPageHeight();
        this.marginLeft = setting.getMarginLeft();
        this.marginRight = setting.getMarginRight();
        this.marginTop = setting.getMarginTop();
        this.marginBottom = setting.getMarginBottom();
        this.fontSizeTitle = setting.getFontSizeTitle();
        this.fontSizeNormal = setting.getFontSizeNormal();
        this.fontSizeSmall = setting.getFontSizeSmall();
    }
}
