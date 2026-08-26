package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IRevenuService;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.PeriodiciteAbonnement;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.common.dto.ImageDownloadResponse;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.common.service.ValidatorService;
import org.store.notification.application.event.PaiementAbonnementRejectedEvent;
import org.store.notification.application.event.PaiementAbonnementSubmittedEvent;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.paiement.application.service.IMoyenPaiementService;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Manual-payment workflow: the owner submits proof against a pre-generated FACTURE_GENEREE invoice;
 * the admin validates or rejects it. On validation, the Abonnement is activated (first payment) or
 * its dateFin extended by 1 month (renewal), with optional plan switch (prochainPlan).
 */
@Service
@Transactional(readOnly = true)
public class PaiementAbonnementServiceImpl implements IPaiementAbonnementService {

    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final ICouponService couponService;
    private final IUtilisationCouponService utilisationCouponService;
    private final IUploadFileService uploadFileService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final INotificationEventPublisher notificationEventPublisher;
    private final IAuditEventPublisher auditEventPublisher;
    private final IMoyenPaiementService moyenPaiementService;
    private final IRevenuService revenuService;

    public PaiementAbonnementServiceImpl(PaiementAbonnementDomainService paiementAbonnementDomainService,
                                         AbonnementDomainService abonnementDomainService,
                                         ICouponService couponService,
                                         IUtilisationCouponService utilisationCouponService,
                                         IUploadFileService uploadFileService,
                                         SubscriptionAmountCalculator amountCalculator,
                                         ICurrentUserService currentUserService,
                                         ValidatorService validatorService,
                                         INotificationEventPublisher notificationEventPublisher,
                                         IAuditEventPublisher auditEventPublisher,
                                         IMoyenPaiementService moyenPaiementService,
                                         IRevenuService revenuService) {
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.couponService = couponService;
        this.utilisationCouponService = utilisationCouponService;
        this.uploadFileService = uploadFileService;
        this.amountCalculator = amountCalculator;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditEventPublisher = auditEventPublisher;
        this.moyenPaiementService = moyenPaiementService;
        this.revenuService = revenuService;
    }

    /**
     * OWNER submits proof against an existing FACTURE_GENEREE invoice. Transitions it to EN_ATTENTE_VALIDATION.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse payer(UUID paiementId,
                                            PaiementAbonnementRequest paiementAbonnementRequest,
                                            MultipartFile preuve) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);
        ensurePaiementIsFactureGeneree(paiement);

        if (preuve !=null && !preuve.isEmpty()){
            PieceJointe preuveImage = uploadFileService.buildImage(preuve);
            paiement.setPreuve(preuveImage);
        }

        paiement.setDatePaiement(paiementAbonnementRequest.datePaiement());
        paiement.setMoyen(moyenPaiementService.findById(paiementAbonnementRequest.moyenPaiementId()));
        paiement.setReferenceTransaction(paiementAbonnementRequest.referenceTransaction());
        paiement.setStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);

        PaiementAbonnement saved = paiementAbonnementDomainService.save(paiement);

        String sigle = paiement.getAbonnement().getEntreprise() != null
                ? paiement.getAbonnement().getEntreprise().getSigle() : null;
        notificationEventPublisher.publishPaiementSubmitted(
                new PaiementAbonnementSubmittedEvent(saved.getId(), sigle, saved.getMontantFinal()));

        return new PaiementAbonnementResponse(saved);
    }

    /**
     * Admin validates: first payment → activate (ACTIF, dateDebut=today, dateFin=today+1month);
     * renewal → extend dateFin +1 month (with optional plan switch from prochainPlan).
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse validate(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        Abonnement abonnement = paiement.getAbonnement();
        activateOrExtend(abonnement);

        PaiementAbonnement validatedPaiement = paiementAbonnementDomainService.markAsValide(paiement);

        UUID entrepriseId = abonnement.getEntreprise().getId();
        String entrepriseSigle = abonnement.getEntreprise().getSigle();

        notificationEventPublisher.publishPaiementValidated(
                new PaiementAbonnementValidatedEvent(validatedPaiement.getId(), entrepriseId, validatedPaiement.getMontantFinal()));

        revenuService.record(new RevenuRecordCommand(
                abonnement.getEntreprise(),
                validatedPaiement.getDatePaiement(),
                validatedPaiement.getMontantFinal()));

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.PAIEMENT_ABONNEMENT_VALIDATED, AuditEntityType.PAIEMENT_ABONNEMENT,
                validatedPaiement.getId(), entrepriseSigle,
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null, null));

        return new PaiementAbonnementResponse(validatedPaiement);
    }

    /**
     * Admin rejects: marks REJETE with mandatory reason and releases the reserved coupon.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse reject(UUID paiementId, RejectPaiementRequest rejectPaiementRequest) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        releaseReservedCouponIfAny(paiement.getAbonnement().getId());

        UUID rejectEntrepriseId = paiement.getAbonnement().getEntreprise().getId();
        String rejectEntrepriseSigle = paiement.getAbonnement().getEntreprise().getSigle();

        PaiementAbonnement rejectedPaiement = paiementAbonnementDomainService.markAsRejete(paiement, rejectPaiementRequest.motifRejet());

        notificationEventPublisher.publishPaiementRejected(
                new PaiementAbonnementRejectedEvent(rejectedPaiement.getId(), rejectEntrepriseId, rejectPaiementRequest.motifRejet()));

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.PAIEMENT_ABONNEMENT_REJECTED, AuditEntityType.PAIEMENT_ABONNEMENT,
                rejectedPaiement.getId(), rejectEntrepriseSigle,
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null, null));

        return new PaiementAbonnementResponse(rejectedPaiement);
    }

    @Override
    public long countByStatutAndCreatedBetween(String statut, LocalDate startDate, LocalDate endDate) {
        StatutPaiementAbonnement statutEnum = (statut == null || statut.isBlank())
                ? null
                : StatutPaiementAbonnement.valueOf(statut);
        return paiementAbonnementDomainService.countByStatutAndCreatedBetween(statutEnum, startDate, endDate);
    }

    @Override
    public java.math.BigDecimal getRevenueForPeriod(LocalDate startDate, LocalDate endDate) {
        return paiementAbonnementDomainService.sumValidatedRevenueForPeriod(startDate, endDate);
    }

    @Override
    public PaiementAbonnementStatsResponse getStatistiquesPaiement(String startDate, String endDate) {
        return paiementAbonnementDomainService.getStatistiquesPaiement(startDate, endDate);
    }

    @Override
    public java.util.Optional<PaiementAbonnementResponse> findMyPending() {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        if (currentEntrepriseId == null) {
            return java.util.Optional.empty();
        }
        return paiementAbonnementDomainService.findPendingResponseByEntreprise(currentEntrepriseId);
    }

    @Override
    public Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter) {
        validatorService.validate(filter);
        PaiementAbonnementFilter scoped = scopeFilterForNonAdmin(filter);
        return paiementAbonnementDomainService.findResponses(scoped);
    }

    @Override
    public PaiementAbonnementResponse findResponseById(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);
        return new PaiementAbonnementResponse(paiement);
    }

    @Override
    public ImageDownloadResponse getPreuve(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);

        PieceJointe preuve = paiement.getPreuve();
        if (preuve == null) {
            throw new EntityException("paiementAbonnement.preuve.notFound");
        }
        return new ImageDownloadResponse(preuve.getDocument(), preuve.getContentType());
    }

    /** Throws BadArgumentException when the paiement cannot accept a proof (not FACTURE_GENEREE or EN_RETARD). */
    public void ensurePaiementIsFactureGeneree(PaiementAbonnement paiement) {
        boolean payable = paiement.getStatut() == StatutPaiementAbonnement.FACTURE_GENEREE
                       || paiement.getStatut() == StatutPaiementAbonnement.EN_RETARD;
        if (!payable) {
            throw new BadArgumentException("paiementAbonnement.notFactureGeneree", paiement.getStatut().name());
        }
    }

    /** Throws BadArgumentException when the paiement is not EN_ATTENTE_VALIDATION. */
    public void ensurePaiementIsPendingValidation(PaiementAbonnement paiement) {
        if (paiement.getStatut() != StatutPaiementAbonnement.EN_ATTENTE_VALIDATION) {
            throw new BadArgumentException("paiementAbonnement.notPendingValidation");
        }
    }

    /**
     * First payment (EN_ATTENTE): expires any running TRIAL, then activates — dateDebut=today, dateFin=today+periodicite.
     * Reactivation (SUSPENDU): activate without touching the TRIAL (already expired at first activation).
     * Renewal (ACTIF): apply prochainPlan + prochainePeriodicite if set, then extend dateFin by periodicite.
     */
    public void activateOrExtend(Abonnement abonnement) {
        if (abonnement.getStatut() == AbonnementStatut.EN_ATTENTE || abonnement.getStatut() == AbonnementStatut.SUSPENDU) {
            if (abonnement.getStatut() == AbonnementStatut.EN_ATTENTE) {
                abonnementDomainService.expireTrialIfAny(abonnement.getEntreprise().getId());
            }
            LocalDate dateDebut = LocalDate.now();
            int mois = abonnement.getPeriodicite() != null ? abonnement.getPeriodicite().getNombreMois() : 1;
            abonnementDomainService.activate(abonnement, dateDebut, dateDebut.plusMonths(mois));
        } else {
            if (abonnement.getProchainPlan() != null) {
                abonnement.setPlanAbonnement(abonnement.getProchainPlan());
                abonnement.setProchainPlan(null);
            }
            if (abonnement.getProchainePeriodicite() != null) {
                abonnement.setPeriodicite(abonnement.getProchainePeriodicite());
                abonnement.setProchainePeriodicite(null);
            }
            int mois = abonnement.getPeriodicite() != null ? abonnement.getPeriodicite().getNombreMois() : 1;
            abonnement.setDateFin(abonnement.getDateFin().plusMonths(mois));
            abonnementDomainService.save(abonnement);
        }
    }

    /** Releases the reserved coupon (decrement + delete UtilisationCoupon). */
    public void releaseReservedCouponIfAny(UUID abonnementId) {
        utilisationCouponService.findCouponIdByAbonnementId(abonnementId).ifPresent(couponId -> {
            Coupon coupon = couponService.findById(couponId);
            couponService.decrementUsage(coupon);
            utilisationCouponService.deleteByAbonnementId(abonnementId);
        });
    }

    public PaiementAbonnementFilter scopeFilterForNonAdmin(PaiementAbonnementFilter filter) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return filter;
        }
        return new PaiementAbonnementFilter(
                filter.statut(), filter.abonnementId(), currentUser.entrepriseId(),
                filter.startDate(), filter.endDate(),
                filter.page(), filter.size());
    }

    @Override
    @Transactional
    public PaiementAbonnement createFactureGeneree(FactureGenereeCommand command) {
        return paiementAbonnementDomainService.createFactureGeneree(command);
    }

    @Override
    public java.util.Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId) {
        return paiementAbonnementDomainService.findFactureNonPayeeByAbonnement(abonnementId);
    }

    /** Recalculates tarif, coupon and amounts on an unpaid invoice after a plan/periodicite change. */
    @Override
    @Transactional
    public void recalculerFactureNonPayee(PaiementAbonnement facture, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown) {
        ensurePaiementIsFactureGeneree(facture);
        paiementAbonnementDomainService.recalculer(facture, inputs, breakdown);
    }

    /** Delegates to the domain service to fetch FACTURE_GENEREE invoices due on any of the given alert dates. */
    @Override
    public java.util.List<PaiementAbonnement> findFacturesAbonnementDues(java.util.List<java.time.LocalDate> dates) {
        return paiementAbonnementDomainService.findFacturesAbonnementDues(dates);
    }

    @Override
    public java.util.List<PaiementAbonnement> findOverdueInvoices(java.time.LocalDate cutoffDate) {
        return paiementAbonnementDomainService.findOverdueInvoices(cutoffDate);
    }

    @Override
    public PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement) {
        return paiementAbonnementDomainService.markAsEnRetard(paiement);
    }

    public void ensurePaiementAccessibleByCaller(PaiementAbonnement paiement) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return;
        }
        if (!paiement.getAbonnement().getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("abonnement.notOwned");
        }
    }

    @Override
    public long countByStatut(StatutPaiementAbonnement statut) {
        return paiementAbonnementDomainService.countByStatut(statut);
    }

    @Override
    public java.math.BigDecimal sumValidatedRevenueForYear(int year) {
        return paiementAbonnementDomainService.sumValidatedRevenueForYear(year);
    }
}
