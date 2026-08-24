package org.store.abonnement.application.dto;

import org.store.entreprise.domain.model.Entreprise;

import java.math.BigDecimal;
import java.time.LocalDate;

public record RevenuRecordCommand(Entreprise entreprise, LocalDate datePaiement, BigDecimal montant) {}
