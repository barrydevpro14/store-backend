package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.FactureGenereeCommand;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.common.dto.ImageDownloadResponse;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public interface IPaiementAbonnementService {

    /**
     * OWNER soumet sa preuve de paiement contre une facture FACTURE_GENEREE existante.
     * Passe le PaiementAbonnement en EN_ATTENTE_VALIDATION. L'abonnement reste EN_ATTENTE jusqu'à la validation admin.
     */
    PaiementAbonnementResponse payer(UUID paiementId, PaiementAbonnementRequest paiementAbonnementRequest, MultipartFile preuve);

    /**
     * ADMIN valide le paiement : statut → VALIDE, et l'abonnement passe en ACTIF avec `dateDebut`/`dateFin` calculés
     * (today si pas d'abonnement actif courant, sinon `currentActif.dateFin + 1`). dateFin = dateDebut + 1 mois.
     */
    PaiementAbonnementResponse validate(UUID paiementId);

    /**
     * ADMIN rejette le paiement : statut → REJETE avec motif obligatoire. L'abonnement reste EN_ATTENTE.
     * Libère le coupon réservé à la souscription (rollback : supprime `UtilisationCoupon` + décrémente `nombreUtilisations`).
     */
    PaiementAbonnementResponse reject(UUID paiementId, RejectPaiementRequest rejectPaiementRequest);

    /**
     * Listing paginé filtré (ADMIN voit tout, OWNER auto-scopé sur son entreprise).
     */
    Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter);

    /**
     * Lecture par id en `Response`. Scoping sur l'entreprise du caller (sauf ADMIN).
     */
    PaiementAbonnementResponse findResponseById(UUID paiementId);

    /**
     * Téléchargement de la preuve d'image. Scoping sur l'entreprise du caller (sauf ADMIN).
     */
    ImageDownloadResponse getPreuve(UUID paiementId);

    /** ADMIN count — payments matching an optional statut and optional dateEcheance range. */
    long countByStatutAndCreatedBetween(String statut, LocalDate startDate, LocalDate endDate);

    /** ADMIN count — payments with the given statut (all periods). */
    long countByStatut(StatutPaiementAbonnement statut);

    /** ADMIN aggregate — sum of montantFinal for VALIDE payments in the given calendar year. */
    java.math.BigDecimal sumValidatedRevenueForYear(int year);

    /** ADMIN aggregate — sum of montantFinal for VALIDE payments in the given dateEcheance range. */
    java.math.BigDecimal getRevenueForPeriod(LocalDate startDate, LocalDate endDate);

    PaiementAbonnementStatsResponse getStatistiquesPaiement(String startDate, String endDate);

    /**
     * Returns the caller's currently pending Paiement (statut EN_ATTENTE_VALIDATION on the
     * EN_ATTENTE Abonnement), or empty when none. Used by the OWNER dashboard to decide whether
     * to keep the "soumettre un paiement" CTA visible or replace it with a "en cours de validation"
     * banner.
     */
    java.util.Optional<PaiementAbonnementResponse> findMyPending();

    /**
     * Creates a FACTURE_GENEREE invoice. Stores tarif + coupon snapshots on the payment row.
     */
    PaiementAbonnement createFactureGeneree(FactureGenereeCommand command);

    /** Finds FACTURE_GENEREE invoices due on any of the given alert dates (daily scheduler use). */
    List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates);

    /** Finds invoices overdue as of cutoffDate (FACTURE_GENEREE or EN_ATTENTE_VALIDATION with dateEcheance < cutoffDate). */
    List<PaiementAbonnement> findOverdueInvoices(LocalDate cutoffDate);

    /** Marks the invoice EN_RETARD (suspension scheduler use). */
    PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement);

    /**
     * Returns the most recent FACTURE_GENEREE or EN_RETARD invoice for the abonnement, or empty
     * when the owner has already submitted proof (EN_ATTENTE_VALIDATION) or none exists.
     */
    java.util.Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId);

    /**
     * Recalculates tarif, coupon and amounts on an existing unpaid invoice after a plan/periodicite change.
     * Only callable on FACTURE_GENEREE or EN_RETARD invoices.
     */
    void recalculerFactureNonPayee(PaiementAbonnement facture, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown);

    /** Throws ForbiddenException when the caller is not ADMIN and does not own the paiement's entreprise. */
    void ensurePaiementAccessibleByCaller(PaiementAbonnement paiement);

    /** Throws BadArgumentException when the paiement is not FACTURE_GENEREE (cannot accept proof). */
    void ensurePaiementIsFactureGeneree(PaiementAbonnement paiement);

    /** Throws BadArgumentException when the paiement is not EN_ATTENTE_VALIDATION. */
    void ensurePaiementIsPendingValidation(PaiementAbonnement paiement);

    /**
     * First payment (EN_ATTENTE): activate with dateDebut=today, dateFin=today+1month.
     * Renewal (ACTIF): extend dateFin +1 month, applying prochainPlan if set.
     */
    void activateOrExtend(Abonnement abonnement);

    /** Releases the reserved coupon for the given abonnement: decrements usage and deletes UtilisationCoupon. */
    void releaseReservedCouponIfAny(UUID abonnementId);

    /** Returns filter unchanged for ADMIN; forces entrepriseId to the caller's entreprise for non-ADMIN. */
    PaiementAbonnementFilter scopeFilterForNonAdmin(PaiementAbonnementFilter filter);
}
