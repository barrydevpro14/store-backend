package org.store.abonnement.application.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public interface IRevenuService {

    /** Persists one Revenu row. Called only from RevenuEventListener, on a validated payment. */
    void record(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant);

    BigDecimal getTotalForPeriod(String startDate, String endDate, UUID countryId, UUID abonnementId);
}
