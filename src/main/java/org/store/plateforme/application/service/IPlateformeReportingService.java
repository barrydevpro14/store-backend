package org.store.plateforme.application.service;

import org.store.plateforme.application.dto.PlateformePeriodFilter;
import org.store.plateforme.application.dto.PlateformePeriodReportResponse;

public interface IPlateformeReportingService {
    PlateformePeriodReportResponse getPeriodReport(PlateformePeriodFilter filter);
}
