package org.store.stock.application.service;

import org.springframework.data.domain.Page;
import org.store.stock.application.dto.ExpiringLotResponse;
import org.store.stock.application.dto.ExpiringLotsFilter;

public interface IExpiringLotsService {

    /**
     * Liste paginée des lots qui expirent dans les daysAhead jours à venir, triés par
     * dateExpiration ASC, scopée sur le magasin du caller. Seuls les lots avec
     * quantiteRestante &gt; 0 et dateExpiration non null sont retournés.
     */
    Page<ExpiringLotResponse> findExpiringLots(ExpiringLotsFilter expiringLotsFilter);
}
