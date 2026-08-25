package org.store.plateforme.application.dto;

import java.math.BigDecimal;

public record PlateformePeriodReportResponse(BigDecimal revenu, BigDecimal depensesPlateforme, BigDecimal benefice) {}
