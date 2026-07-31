package org.store.magasin.application.service;

import org.store.magasin.application.dto.MagasinStatsResponse;

import java.util.UUID;

public interface IMagasinStatsService {

    /** Returns current-month KPIs (employees, clients, stock, revenue) for the given magasin. */
    MagasinStatsResponse getStats(UUID magasinId);
}
