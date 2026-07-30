package org.store.reporting.application.service;

import org.store.magasin.application.dto.MagasinStatsRow;
import org.store.reporting.application.dto.AdminOverviewStatsResponse;
import org.store.reporting.application.dto.PeriodReportResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IAdminReportingService {

    /** Returns all KPI counts shown on the admin reporting overview in a single call. */
    AdminOverviewStatsResponse getOverviewStats();

    /** Returns the 4 period KPIs (new subscriptions, validated/rejected payments, revenue) in a single call. */
    PeriodReportResponse getPeriodStats(String startDate, String endDate);

    /** Returns per-store employee stats (actifs/inactifs) for the given entreprise. */
    List<MagasinStatsRow> getEntrepriseStats(UUID entrepriseId);
}
