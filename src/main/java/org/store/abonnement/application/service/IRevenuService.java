package org.store.abonnement.application.service;

import org.store.abonnement.application.dto.RevenuPeriodFilter;
import org.store.abonnement.application.dto.RevenuRecordCommand;

import java.math.BigDecimal;

public interface IRevenuService {

    /** Persists one Revenu row. Called synchronously from PaiementAbonnementServiceImpl.validate(), within the same transaction. */
    void record(RevenuRecordCommand command);

    BigDecimal getTotalForPeriod(RevenuPeriodFilter filter);
}
