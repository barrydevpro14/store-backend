package org.store.abonnement.application.service.impl;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.application.service.IRevenuService;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.notification.application.event.PaiementAbonnementValidatedEvent;
import org.store.notification.application.service.INotificationEventPublisher;
import org.store.paiement.application.dto.FacturationOptionResponse;
import org.store.paiement.application.service.IFacturationService;
import org.store.audit.application.event.AuditEvent;
import org.store.audit.application.service.IAuditEventPublisher;
import org.store.audit.domain.enums.AuditAction;
import org.store.audit.domain.enums.AuditEntityType;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.enums.PermissionCode;
import org.store.security.application.service.ICurrentUserService;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Manages FACTURE_GENEREE invoices: creation (billing scheduler), listing, overdue detection,
 * and confirmation once a PreuvePaiement is validated (see IPreuvePaiementService.validate()).
 */
@Service
@Transactional(readOnly = true)
public class PaiementAbonnementServiceImpl implements IPaiementAbonnementService {

    private final PaiementAbonnementDomainService paiementAbonnementDomainService;
    private final AbonnementDomainService abonnementDomainService;
    private final ICurrentUserService currentUserService;
    private final ValidatorService validatorService;
    private final INotificationEventPublisher notificationEventPublisher;
    private final IAuditEventPublisher auditEventPublisher;
    private final IRevenuService revenuService;
    private final IFacturationService facturationService;

    public PaiementAbonnementServiceImpl(PaiementAbonnementDomainService paiementAbonnementDomainService,
                                         AbonnementDomainService abonnementDomainService,
                                         ICurrentUserService currentUserService,
                                         ValidatorService validatorService,
                                         INotificationEventPublisher notificationEventPublisher,
                                         IAuditEventPublisher auditEventPublisher,
                                         IRevenuService revenuService,
                                         IFacturationService facturationService) {
        this.paiementAbonnementDomainService = paiementAbonnementDomainService;
        this.abonnementDomainService = abonnementDomainService;
        this.currentUserService = currentUserService;
        this.validatorService = validatorService;
        this.notificationEventPublisher = notificationEventPublisher;
        this.auditEventPublisher = auditEventPublisher;
        this.revenuService = revenuService;
        this.facturationService = facturationService;
    }

    @Override
    @Transactional
    public PaiementAbonnementResponse confirmPaiement(UUID paiementId, LocalDate datePaiement) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementIsFactureGeneree(paiement);

        Abonnement abonnement = paiement.getAbonnement();
        activateOrExtend(abonnement);

        PaiementAbonnement validatedPaiement = paiementAbonnementDomainService.markAsValide(paiement, datePaiement);

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

    @Override
    public long countByStatutAndCreatedBetween(String statut, LocalDate startDate, LocalDate endDate) {
        StatutPaiementAbonnement statutEnum = parseStatutOrThrow(statut);
        return paiementAbonnementDomainService.countByStatutAndCreatedBetween(statutEnum, startDate, endDate);
    }

    /**
     * Validates the raw statut string against the enum before parsing — same membership check as
     * the {@code @EnumValue} annotation used on {@link PaiementAbonnementFilter#statut()} — and
     * throws BadArgumentException (400) instead of letting an unguarded valueOf() surface as a 500.
     */
    private StatutPaiementAbonnement parseStatutOrThrow(String statut) {
        if (statut == null || statut.isBlank()) {
            return null;
        }
        boolean isValidStatut = Arrays.stream(StatutPaiementAbonnement.values())
                .anyMatch(candidate -> candidate.name().equals(statut));
        if (!isValidStatut) {
            throw new BadArgumentException("paiementAbonnement.invalidStatut", statut);
        }
        return StatutPaiementAbonnement.valueOf(statut);
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
    public long countPendingFactures() {
        return paiementAbonnementDomainService.countPendingFactures();
    }

    @Override
    public Optional<PaiementAbonnementDetailsResponse> findMyPending() {
        UUID currentEntrepriseId = currentUserService.getCurrent().entrepriseId();
        if (currentEntrepriseId == null) {
            return Optional.empty();
        }
        return paiementAbonnementDomainService.findCurrentUnpaidFactureByEntreprise(currentEntrepriseId);
    }

    @Override
    public Page<PaiementAbonnementResponse> findAll(PaiementAbonnementFilter filter) {
        validatorService.validate(filter);
        PaiementAbonnementFilter scoped = scopeFilterForNonAdmin(filter);
        return paiementAbonnementDomainService.findResponses(scoped);
    }

    /**
     * Plain entity fetch + lazy-loaded preuves within this method's open transaction — matches
     * the PaiementAbonnementDetailsResponse(PaiementAbonnement) constructor pattern (Task 5)
     * without needing a dedicated JPQL query for a single-id lookup.
     */
    @Override
    public PaiementAbonnementDetailsResponse findDetailsById(UUID paiementId) {
        PaiementAbonnement paiement = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(paiement);
        return new PaiementAbonnementDetailsResponse(paiement);
    }

    /** Resolves the facture's own entreprise country, then delegates to the facturation options lookup. */
    @Override
    public List<FacturationOptionResponse> findFacturationOptions(UUID paiementId) {
        PaiementAbonnement facture = paiementAbonnementDomainService.findById(paiementId);
        ensurePaiementAccessibleByCaller(facture);

        UUID countryId = facture.getAbonnement().getEntreprise().getCountry().getId();
        return facturationService.findSelectOptions(countryId);
    }

    @Override
    public void ensurePaiementIsFactureGeneree(PaiementAbonnement paiement) {
        boolean payable = paiement.getStatut() == StatutPaiementAbonnement.FACTURE_GENEREE
                       || paiement.getStatut() == StatutPaiementAbonnement.EN_RETARD;
        if (!payable) {
            throw new BadArgumentException("paiementAbonnement.notFactureGeneree", paiement.getStatut().name());
        }
    }

    @Override
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

    @Override
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
    public Optional<PaiementAbonnement> findFactureNonPayeeByAbonnement(UUID abonnementId) {
        return paiementAbonnementDomainService.findFactureNonPayeeByAbonnement(abonnementId);
    }

    @Override
    @Transactional
    public void recalculerFactureNonPayee(PaiementAbonnement facture, SubscriptionAmountInputs inputs, SubscriptionAmountBreakdown breakdown) {
        ensurePaiementIsFactureGeneree(facture);
        paiementAbonnementDomainService.recalculer(facture, inputs, breakdown);
    }

    @Override
    public List<PaiementAbonnement> findFacturesAbonnementDues(List<LocalDate> dates) {
        return paiementAbonnementDomainService.findFacturesAbonnementDues(dates);
    }

    @Override
    public List<PaiementAbonnement> findOverdueInvoices(LocalDate cutoffDate) {
        return paiementAbonnementDomainService.findOverdueInvoices(cutoffDate);
    }

    @Override
    public PaiementAbonnement markAsEnRetard(PaiementAbonnement paiement) {
        return paiementAbonnementDomainService.markAsEnRetard(paiement);
    }

    @Override
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
    public java.math.BigDecimal sumValidatedRevenueForYear(int year) {
        return paiementAbonnementDomainService.sumValidatedRevenueForYear(year);
    }
}
