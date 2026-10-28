package org.store.common.dto;

import java.math.BigDecimal;

/** Unpaid invoices (vente or achat) over a period: how many, and the amount still due (total − paid). */
public record UnpaidInvoicesSummaryResponse(
        long nombre,
        BigDecimal montantRestant
) {
    public UnpaidInvoicesSummaryResponse(Long nombre, BigDecimal montantRestant) {
        this(
                nombre != null ? nombre : 0L,
                montantRestant != null ? montantRestant : BigDecimal.ZERO
        );
    }
}
