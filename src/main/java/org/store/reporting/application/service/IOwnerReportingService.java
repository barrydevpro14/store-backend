package org.store.reporting.application.service;

import org.store.reporting.application.dto.OwnerOverviewStatsResponse;

public interface IOwnerReportingService {
    /** Returns all company-wide KPIs for the current OWNER in a single transactional call. */
    OwnerOverviewStatsResponse getOwnerOverviewStats();
}
