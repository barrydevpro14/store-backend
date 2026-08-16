package org.store.abonnement.application.service.impl;

import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PlanAbonnementTarif;

import java.time.LocalDate;

/**
 * Regroupe les données nécessaires à la création d'une FACTURE_GENEREE
 * (respect de la règle max 3 paramètres par méthode).
 */
public record FactureGenereeCommand(
        Abonnement abonnement,
        PlanAbonnementTarif tarif,
        Coupon coupon,
        SubscriptionAmountBreakdown breakdown,
        LocalDate dateEcheance
) {}
