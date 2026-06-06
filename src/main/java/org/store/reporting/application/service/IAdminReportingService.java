package org.store.reporting.application.service;

import org.store.reporting.application.dto.AdminOverviewStatsResponse;

public interface IAdminReportingService {

    /** Returns all KPI counts shown on the admin reporting overview in a single call. */
    AdminOverviewStatsResponse getOverviewStats();
}
