package org.store.reporting.application.dto;

import java.math.BigDecimal;

/** Company-wide KPI dashboard for OWNER — aggregates across all the company's magasins. */
public record OwnerOverviewStatsResponse(
        long ventesTodayCount,
        BigDecimal ventesTodayTotal,
        long stockBelowThresholdCount,
        long achatsEnAttente,
        long facturesImpayees
) {}
