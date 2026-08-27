package org.store.reporting.application.dto;

import java.math.BigDecimal;

public record PeriodReportResponse(
        long nouveauxAbonnements,
        long paiementsValides,
        BigDecimal revenu
) {}
