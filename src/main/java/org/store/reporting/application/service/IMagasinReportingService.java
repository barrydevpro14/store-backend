package org.store.reporting.application.service;

import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;

public interface IMagasinReportingService {

    /** Aggregates all magasin KPIs for a given period into a single response. */
    MagasinOverviewStatsResponse getOverview(MagasinOverviewFilter magasinOverviewFilter);
}
