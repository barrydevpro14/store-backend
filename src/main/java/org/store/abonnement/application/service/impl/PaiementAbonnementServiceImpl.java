package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.TypePlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;
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
 * Manual-payment workflow: the owner registers a payment with a mandatory proof image, the admin validates
 * or rejects it. On validation, the Abonnement is activated (dateDebut/dateFin computed via the
 * dateFin-replacement strategy). On rejection, the coupon reserved at subscribe time is released.
 */
@Service
@Transactional(readOnly = true)
public class PaiementAbonnementServiceImpl implements IPaiementAbonnementService {

    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final IAbonnementService abonnementService;
    private final PromotionDomainService promotionDomainService;
    private final CouponDomainService couponDomainService;
    private final UtilisationCouponDomainService utilisationCouponDomainService;
    private final IUploadFileService uploadFileService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final INotificationEventPublisher notificationEventPublisher;
    private final IAuditEventPublisher auditEventPublisher;
    private final IMoyenPaiementService moyenPaiementService;

    public PaiementAbonnementServiceImpl(PaiementAbonnementDomainService paiementAbonnementDomainService,
                                         AbonnementDomainService abonnementDomainService,
                                         IAbonnementService abonnementService,
                                         PromotionDomainService promotionDomainService,
                                         CouponDomainService couponDomainService,
                                         UtilisationCouponDomainService utilisationCouponDomainService,
                                         IUploadFileService uploadFileService,
                                         SubscriptionAmountCalculator amountCalculator,
                                         ICurrentUserService currentUserService,
                                         ValidatorService validatorService,
                                         INotificationEventPublisher notificationEventPublisher,
                                         IAuditEventPublisher auditEventPublisher,
                                         IMoyenPaiementService moyenPaiementService) {
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.abonnementService = abonnementService;
        this.promotionDomainService = promotionDomainService;
        this.couponDomainService = couponDomainService;
        this.utilisationCouponDomainService = utilisationCouponDomainService;
        this.uploadFileService = uploadFileService;
        this.amountCalculator = amountCalculator;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditEventPublisher = auditEventPublisher;
        this.moyenPaiementService = moyenPaiementService;
    }

    /**
     * Owner registers a payment: enforces Abonnement EN_ATTENTE + no pending payment already, recomputes the
     * breakdown (plan + type + active promotion + reserved coupon) and persists the PaiementAbonnement in
     * EN_ATTENTE_VALIDATION.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse create(UUID abonnementId,
                                             PaiementAbonnementRequest paiementAbonnementRequest,
                                             MultipartFile preuve) {
        Abonnement abonnement = abonnementService.ensureBelongsToCurrentEntreprise(abonnementDomainService.findById(abonnementId));
        ensureAbonnementIsPending(abonnement);
        ensureNoPendingPayment(abonnementId);

        SubscriptionAmountBreakdown breakdown = recomputeBreakdown(abonnement);
        PieceJointe preuveImage = uploadFileService.buildImage(preuve);

        PaiementAbonnement paiement = paiementAbonnementDomainService.createPending(
                new PaiementAbonnementCreationContext(abonnement, paiementAbonnementRequest, breakdown, preuveImage,
                        moyenPaiementService.findById(paiementAbonnementRequest.moyenPaiementId())));

        notificationEventPublisher.publishPaiementSubmitted(
                new PaiementAbonnementSubmittedEvent(paiement));

        return new PaiementAbonnementResponse(paiement);
    }

    /**
     * Admin validates: marks the payment VALIDE and activates the Abonnement (statut=ACTIF, dateDebut/dateFin
     * computed). Strategy: if an active Abonnement already exists for the entreprise, the new one starts at
     * {@code currentActif.dateFin + 1}; otherwise it starts today. dateFin = dateDebut + type.dureeMois.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse validate(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        Abonnement abonnement = paiement.getAbonnement();
        activateAbonnement(abonnement);

        PaiementAbonnement validatedPaiement = paiementAbonnementDomainService.markAsValide(paiement);

        notificationEventPublisher.publishPaiementValidated(new PaiementAbonnementValidatedEvent(validatedPaiement));

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.PAIEMENT_ABONNEMENT_VALIDATED, AuditEntityType.PAIEMENT_ABONNEMENT,
                validatedPaiement.getId(), abonnement.getEntreprise().getSigle(),
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null, null));

        return new PaiementAbonnementResponse(validatedPaiement);
    }

    /**
     * Admin rejects: marks the payment REJETE with a mandatory reason and releases the reserved coupon
     * (removes UtilisationCoupon + decrements coupon.nombreUtilisations).
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse reject(UUID paiementId, RejectPaiementRequest rejectPaiementRequest) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        releaseReservedCouponIfAny(paiement.getAbonnement().getId());

        PaiementAbonnement rejectedPaiement = paiementAbonnementDomainService.markAsRejete(paiement, rejectPaiementRequest.motifRejet());

        notificationEventPublisher.publishPaiementRejected(new PaiementAbonnementRejectedEvent(rejectedPaiement));

        UserPrincipal caller = currentUserService.getCurrent();
        auditEventPublisher.publish(new AuditEvent(
                AuditAction.PAIEMENT_ABONNEMENT_REJECTED, AuditEntityType.PAIEMENT_ABONNEMENT,
                rejectedPaiement.getId(), rejectedPaiement.getAbonnement().getEntreprise().getSigle(),
                caller.accountId().toString(), caller.username(), caller.entrepriseId(), null, null));

        return new PaiementAbonnementResponse(rejectedPaiement);
    }

    /** ADMIN count — no auto-scoping; counts payments matching an optional statut + date range. */
    @Override
    public long countByStatutAndCreatedBetween(String statut, String startDate, String endDate) {
        StatutPaiementAbonnement statutEnum = (statut == null || statut.isBlank())
                ? null
                : StatutPaiementAbonnement.valueOf(statut);
        return paiementAbonnementDomainService.countByStatutAndCreatedBetween(statutEnum, startDate, endDate);
    }

    /** Paginated filtered listing; auto-scoped to the caller's entreprise when the caller is not ADMIN. */
    @Override
    public Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter) {
        validatorService.validate(filter);
        PaiementAbonnementFilter scoped = scopeFilterForNonAdmin(filter);
        return paiementAbonnementDomainService.findResponses(scoped);
    }

    /** Returns a payment as a Response after the scoping check. */
    @Override
    public PaiementAbonnementResponse findResponseById(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);
        return new PaiementAbonnementResponse(paiement);
    }

    /** Downloads the proof image attached to the payment (404 when no proof). */
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

    /** Throws {@code BadArgumentException} when the Abonnement is not in EN_ATTENTE (already active or otherwise). */
    public void ensureAbonnementIsPending(Abonnement abonnement) {
        if (abonnement.getStatut() != AbonnementStatut.EN_ATTENTE) {
            throw new BadArgumentException("abonnement.notPending");
        }
    }

    /** Throws {@code BadArgumentException} when a payment is already EN_ATTENTE_VALIDATION for this Abonnement. */
    public void ensureNoPendingPayment(UUID abonnementId) {
        if (paiementAbonnementDomainService.existsPendingForAbonnement(abonnementId)) {
            throw new BadArgumentException("paiementAbonnement.alreadyPending");
        }
    }

    /** Throws {@code BadArgumentException} when the payment is not EN_ATTENTE_VALIDATION (already validated or rejected). */
    public void ensurePaiementIsPendingValidation(PaiementAbonnement paiement) {
        if (paiement.getStatut() != StatutPaiementAbonnement.EN_ATTENTE_VALIDATION) {
            throw new BadArgumentException("paiementAbonnement.notPendingValidation");
        }
    }

    /** Recomputes the breakdown at payment time (active promotion at {@code today}, coupon from {@code UtilisationCoupon}). */
    public SubscriptionAmountBreakdown recomputeBreakdown(Abonnement abonnement) {
        TypePlanAbonnement type = abonnement.getTypePlanAbonnement();
        PlanAbonnement plan = type.getPlan();

        Promotion promotion = promotionDomainService
                .findFirstActivePromotionForPlan(plan.getId(), LocalDate.now())
                .orElse(null);

        Coupon coupon = utilisationCouponDomainService.findCouponIdByAbonnementId(abonnement.getId())
                .map(couponDomainService::findById)
                .orElse(null);

        return amountCalculator.calculate(new SubscriptionAmountInputs(plan, type, promotion, coupon));
    }

    /** Computes {@code dateDebut} (today or {@code currentActif.dateFin+1}) then {@code dateFin} (+ dureeMois) and delegates the activation to the domain service. */
    public void activateAbonnement(Abonnement abonnement) {
        UUID entrepriseId = abonnement.getEntreprise().getId();

        LocalDate dateDebut = abonnementDomainService
                .findLatestActifDateFin(entrepriseId, abonnement.getId())
                .map(latestDateFin -> latestDateFin.plusDays(1))
                .orElse(LocalDate.now());

        LocalDate dateFin = dateDebut.plusMonths(abonnement.getTypePlanAbonnement().getDureeMois());

        abonnementDomainService.activate(abonnement, dateDebut, dateFin);
    }

    /** Releases the reserved coupon: decrements {@code coupon.nombreUtilisations} then deletes the {@code UtilisationCoupon} via bulk delete. */
    public void releaseReservedCouponIfAny(UUID abonnementId) {
        utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId).ifPresent(couponId -> {
            Coupon coupon = couponDomainService.findById(couponId);
            couponDomainService.decrementUsage(coupon);
            utilisationCouponDomainService.deleteByAbonnementId(abonnementId);
        });
    }

    /** Auto-scopes the filter to the caller's entreprise when the caller is not ADMIN. */
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

    /** ADMIN sees everything; otherwise the payment must belong to the caller's entreprise. */
    public void ensurePaiementAccessibleByCaller(PaiementAbonnement paiement) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return;
        }
        if (!paiement.getAbonnement().getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("abonnement.notOwned");
        }
    }
}
