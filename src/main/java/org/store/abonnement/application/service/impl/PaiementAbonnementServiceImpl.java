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
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.Promotion;
import org.store.abonnement.domain.model.UtilisationCoupon;
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
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.util.UUID;

/**
 * Workflow de paiement manuel : le propriétaire enregistre son paiement avec preuve image obligatoire,
 * l'admin valide ou rejette. À la validation, l'abonnement est activé (dateDebut/dateFin calculés selon
 * la stratégie de remplacement à dateFin). Au rejet, le coupon réservé à la souscription est libéré.
 */
@Service
@Transactional(readOnly = true)
public class PaiementAbonnementServiceImpl implements IPaiementAbonnementService {

    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final PromotionDomainService promotionDomainService;
    private final CouponDomainService couponDomainService;
    private final UtilisationCouponDomainService utilisationCouponDomainService;
    private final IUploadFileService uploadFileService;
    private final SubscriptionAmountCalculator amountCalculator;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;

    public PaiementAbonnementServiceImpl(PaiementAbonnementDomainService paiementAbonnementDomainService,
                                         AbonnementDomainService abonnementDomainService,
                                         PromotionDomainService promotionDomainService,
                                         CouponDomainService couponDomainService,
                                         UtilisationCouponDomainService utilisationCouponDomainService,
                                         IUploadFileService uploadFileService,
                                         SubscriptionAmountCalculator amountCalculator,
                                         ICurrentUserService currentUserService,
                                         ValidatorService validatorService) {
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.promotionDomainService = promotionDomainService;
        this.couponDomainService = couponDomainService;
        this.utilisationCouponDomainService = utilisationCouponDomainService;
        this.uploadFileService = uploadFileService;
        this.amountCalculator = amountCalculator;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
    }

    /**
     * PROPRIETAIRE enregistre son paiement : vérifie abonnement EN_ATTENTE + pas de paiement pendant déjà,
     * recalcule le breakdown (plan + type + promotion active + coupon réservé) puis crée PaiementAbonnement EN_ATTENTE_VALIDATION.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse create(UUID abonnementId,
                                             PaiementAbonnementRequest paiementAbonnementRequest,
                                             MultipartFile preuve) {
        Abonnement abonnement = ensureAbonnementBelongsToCurrentEntreprise(abonnementDomainService.findById(abonnementId));
        ensureAbonnementIsPending(abonnement);
        ensureNoPendingPayment(abonnementId);

        SubscriptionAmountBreakdown breakdown = recomputeBreakdown(abonnement);
        PieceJointe preuveImage = uploadFileService.buildImage(preuve);

        PaiementAbonnement paiement = paiementAbonnementDomainService.createPending(
                new PaiementAbonnementCreationContext(abonnement, paiementAbonnementRequest, breakdown, preuveImage));

        return new PaiementAbonnementResponse(paiement);
    }

    /**
     * ADMIN valide : passe le paiement en VALIDE et active l'abonnement (statut=ACTIF, dateDebut/dateFin calculés).
     * Stratégie : si un abonnement actif existe déjà pour l'entreprise, le nouveau démarre `currentActif.dateFin + 1`,
     * sinon il démarre aujourd'hui. dateFin = dateDebut + typeAbonnement.dureeMois.
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse validate(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        Abonnement abonnement = paiement.getAbonnement();
        activateAbonnement(abonnement);

        return new PaiementAbonnementResponse(paiementAbonnementDomainService.markAsValide(paiement));
    }

    /**
     * ADMIN rejette : passe le paiement en REJETE avec motif obligatoire. Libère le coupon réservé
     * (supprime UtilisationCoupon + décrémente coupon.nombreUtilisations).
     */
    @Override
    @Transactional
    public PaiementAbonnementResponse reject(UUID paiementId, RejectPaiementRequest rejectPaiementRequest) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsPendingValidation(paiement);

        releaseReservedCouponIfAny(paiement.getAbonnement().getId());

        return new PaiementAbonnementResponse(
                paiementAbonnementDomainService.markAsRejete(paiement, rejectPaiementRequest.motifRejet()));
    }

    /** Listing paginé filtré ; auto-scopé à l'entreprise du caller s'il n'est pas ADMIN. */
    @Override
    public Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter) {
        validatorService.validate(filter);
        PaiementAbonnementFilter scoped = scopeFilterForNonAdmin(filter);
        return paiementAbonnementDomainService.findResponses(scoped);
    }

    /** Retourne un paiement en `Response` après vérification de scoping. */
    @Override
    public PaiementAbonnementResponse findResponseById(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);
        return new PaiementAbonnementResponse(paiement);
    }

    /** Télécharge la preuve d'image associée au paiement (404 si pas de preuve). */
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

    /** Lève `ForbiddenException` si l'abonnement n'appartient pas à l'entreprise du caller. */
    public Abonnement ensureAbonnementBelongsToCurrentEntreprise(Abonnement abonnement) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (!abonnement.getEntreprise().getId().equals(currentUser.entrepriseId())) {
            throw new ForbiddenException("abonnement.notOwned");
        }
        return abonnement;
    }

    /** Lève `BadArgumentException` si l'abonnement n'est pas en EN_ATTENTE (déjà actif ou autre). */
    public void ensureAbonnementIsPending(Abonnement abonnement) {
        if (abonnement.getStatut() != AbonnementStatut.EN_ATTENTE) {
            throw new BadArgumentException("abonnement.notPending");
        }
    }

    /** Lève `BadArgumentException` si un paiement EN_ATTENTE_VALIDATION existe déjà pour cet abonnement. */
    public void ensureNoPendingPayment(UUID abonnementId) {
        if (paiementAbonnementDomainService.findFirstPendingByAbonnement(abonnementId).isPresent()) {
            throw new BadArgumentException("paiementAbonnement.alreadyPending");
        }
    }

    /** Lève `BadArgumentException` si le paiement n'est pas en EN_ATTENTE_VALIDATION (déjà validé ou rejeté). */
    public void ensurePaiementIsPendingValidation(PaiementAbonnement paiement) {
        if (paiement.getStatut() != StatutPaiementAbonnement.EN_ATTENTE_VALIDATION) {
            throw new BadArgumentException("paiementAbonnement.notPendingValidation");
        }
    }

    /** Recalcule le breakdown au moment du paiement (promotion active à `today`, coupon depuis `UtilisationCoupon`). */
    public SubscriptionAmountBreakdown recomputeBreakdown(Abonnement abonnement) {
        Promotion promotion = promotionDomainService
                .findFirstActivePromotionForPlan(abonnement.getPlan().getId(), LocalDate.now())
                .orElse(null);

        Coupon coupon = utilisationCouponDomainService.findByAbonnementId(abonnement.getId())
                .map(UtilisationCoupon::getCoupon)
                .orElse(null);

        return amountCalculator.calculate(new SubscriptionAmountInputs(
                abonnement.getPlan(), abonnement.getTypeAbonnement(), promotion, coupon));
    }

    /** Calcule `dateDebut` (today ou `currentActif.dateFin+1`) puis `dateFin` (+ dureeMois) et délègue l'activation au domain service. */
    public void activateAbonnement(Abonnement abonnement) {
        UUID entrepriseId = abonnement.getEntreprise().getId();

        LocalDate dateDebut = abonnementDomainService.findCurrentActif(entrepriseId)
                .filter(currentActif -> !currentActif.getId().equals(abonnement.getId()))
                .map(currentActif -> currentActif.getDateFin() == null ? LocalDate.now() : currentActif.getDateFin().plusDays(1))
                .orElse(LocalDate.now());

        LocalDate dateFin = dateDebut.plusMonths(abonnement.getTypeAbonnement().getDureeMois());

        abonnementDomainService.activate(abonnement, dateDebut, dateFin);
    }

    /** Libère le coupon réservé : décrémente `coupon.nombreUtilisations` puis supprime `UtilisationCoupon`. */
    public void releaseReservedCouponIfAny(UUID abonnementId) {
        utilisationCouponDomainService.findByAbonnementId(abonnementId).ifPresent(utilisation -> {
            Coupon coupon = utilisation.getCoupon();
            if (coupon != null) {
                couponDomainService.decrementUsage(coupon);
            }
            utilisationCouponDomainService.delete(utilisation);
        });
    }

    /** Auto-scope le filter à l'entreprise du caller si non-ADMIN. */
    public PaiementAbonnementFilter scopeFilterForNonAdmin(PaiementAbonnementFilter filter) {
        UserPrincipal currentUser = currentUserService.getCurrent();
        if (currentUser.hasPermission(PermissionCode.ADMIN_ACCESS)) {
            return filter;
        }
        return new PaiementAbonnementFilter(
                filter.statut(), filter.abonnementId(), currentUser.entrepriseId(), filter.page(), filter.size());
    }

    /** ADMIN peut tout voir ; sinon le paiement doit être dans l'entreprise du caller. */
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
