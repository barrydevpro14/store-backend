package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.impl.FactureGenereeCommand;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface IPaiementAbonnementService {

    /**
     * Confirms a facture as paid — statut -> VALIDE, datePaiement set to the given date (the
     * winning preuve's own date), abonnement activated/extended, revenue recorded, notification +
     * audit published. Called by IPreuvePaiementService.validate() once the preuve is VALIDEE.
     */
    PaiementAbonnementResponse confirmPaiement(UUID paiementId, LocalDate datePaiement);

    /** Listing paginé filtré (ADMIN voit tout, OWNER auto-scopé sur son entreprise). */
    Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter);

    /** Facture + historique complet de ses PreuvePaiement. Scoping sur l'entreprise du caller (sauf ADMIN). */
    PaiementAbonnementDetailsResponse findDetailsById(UUID paiementId);

    long countByStatutAndCreatedBetween(String statut, LocalDate startDate, LocalDate endDate);

    java.math.BigDecimal sumValidatedRevenueForYear(int year);

    java.math.BigDecimal getRevenueForPeriod(LocalDate startDate, LocalDate endDate);

    PaiementAbonnementStatsResponse getStatistiquesPaiement(String startDate, String endDate);

    /** All-time count of unpaid factures (FACTURE_GENEREE or EN_RETARD) — admin overview KPI. */
    long countPendingFactures();

    /**
     * Returns the caller's current unpaid facture (FACTURE_GENEREE or EN_RETARD) with its full
     * preuve history, or empty when none exists.
     */
    Optional<PaiementAbonnementDetailsResponse> findMyPending();

    PaiementAbonnement createFactureGeneree(FactureGenereeCommand command);

    List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates);

    List<PaiementAbonnement> findOverdueInvoices(LocalDate cutoffDate);

    PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement);

    Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId);

    void recalculerFactureNonPayee(PaiementAbonnement facture, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown);

    /** Throws ForbiddenException when the caller is not ADMIN and does not own the paiement's entreprise. */
    void ensurePaiementAccessibleByCaller(PaiementAbonnement paiement);

    /** Throws BadArgumentException when the paiement is not FACTURE_GENEREE or EN_RETARD (cannot accept proof). */
    void ensurePaiementIsFactureGeneree(PaiementAbonnement paiement);

    void activateOrExtend(Abonnement abonnement);

    PaiementAbonnementFilter scopeFilterForNonAdmin(PaiementAbonnementFilter filter);
}
