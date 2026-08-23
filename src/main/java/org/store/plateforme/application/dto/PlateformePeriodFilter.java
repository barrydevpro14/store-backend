package org.store.plateforme.application.dto;

import java.util.UUID;

public record PlateformePeriodFilter(String startDate, String endDate, UUID countryId, UUID abonnementId) {}
