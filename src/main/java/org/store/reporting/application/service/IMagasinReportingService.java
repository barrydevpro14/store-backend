package org.store.reporting.application.service;

import org.store.reporting.application.dto.MagasinDashboardStatsResponse;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;

import java.util.UUID;

public interface IMagasinReportingService {

    /** Aggregates all magasin KPIs for a given period into a single response. */
    MagasinOverviewStatsResponse getOverview(MagasinOverviewFilter magasinOverviewFilter);

    /** Returns lightweight today-scoped KPIs for the MANAGER/SELLER dashboard home. */
    MagasinDashboardStatsResponse getDashboardStats(UUID magasinId);
}
