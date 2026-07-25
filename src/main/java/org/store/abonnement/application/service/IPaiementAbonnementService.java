package org.store.abonnement.application.service;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
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

    /** ADMIN count — payments matching an optional statut and optional createdAt date range. */
    long countByStatutAndCreatedBetween(String statut, String startDate, String endDate);

    /**
     * Returns the caller's currently pending Paiement (statut EN_ATTENTE_VALIDATION on the
     * EN_ATTENTE Abonnement), or empty when none. Used by the OWNER dashboard to decide whether
     * to keep the "soumettre un paiement" CTA visible or replace it with a "en cours de validation"
     * banner.
     */
    java.util.Optional<PaiementAbonnementResponse> findMyPending();

    /**
     * Creates a FACTURE_GENEREE invoice for an EN_ATTENTE Abonnement at subscription time.
     * Delegates amount fields from the pre-computed breakdown.
     */
    PaiementAbonnement createFactureGeneree(Abonnement abonnement, SubscriptionAmountBreakdown breakdown, LocalDate dateEcheance);

    /** Finds FACTURE_GENEREE invoices due on any of the given alert dates (daily scheduler use). */
    List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates);

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
