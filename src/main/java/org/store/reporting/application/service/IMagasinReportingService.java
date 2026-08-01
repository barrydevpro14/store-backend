package org.store.reporting.application.service;

import org.store.reporting.application.dto.MagasinDashboardStatsResponse;
import org.store.reporting.application.dto.MagasinOverviewFilter;
import org.store.reporting.application.dto.MagasinOverviewStatsResponse;
import org.store.reporting.application.dto.MagasinVentesStatsResponse;

import java.util.UUID;

public interface IMagasinReportingService {

    /** Aggregates all magasin KPIs for a given period into a single response. */
    MagasinOverviewStatsResponse getOverview(MagasinOverviewFilter magasinOverviewFilter);

    /** Aggregates vente-only KPIs (no achat data) for a given period. */
    MagasinVentesStatsResponse getVentesStats(MagasinOverviewFilter filter);

    /** Returns lightweight today-scoped KPIs for the MANAGER/SELLER dashboard home. */
    MagasinDashboardStatsResponse getDashboardStats(UUID magasinId);
}
