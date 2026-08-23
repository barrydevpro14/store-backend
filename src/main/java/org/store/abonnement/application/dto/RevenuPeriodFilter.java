package org.store.abonnement.application.dto;

import java.util.UUID;

public record RevenuPeriodFilter(String startDate, String endDate, UUID countryId, UUID abonnementId) {}
