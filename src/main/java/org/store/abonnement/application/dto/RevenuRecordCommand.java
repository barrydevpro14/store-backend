package org.store.abonnement.application.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record RevenuRecordCommand(UUID entrepriseId, UUID countryId, LocalDate datePaiement, BigDecimal montant) {}
