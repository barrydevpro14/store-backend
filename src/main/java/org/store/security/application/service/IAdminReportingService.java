package org.store.security.application.service;

import org.store.security.application.dto.AdminOverviewStatsResponse;

public interface IAdminReportingService {

    /** Returns all KPI counts shown on the admin reporting overview in a single call. */
    AdminOverviewStatsResponse getOverviewStats();
}
