package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;
import org.store.abonnement.application.dto.PaiementAbonnementFilter;
import org.store.abonnement.application.dto.PaiementAbonnementRequest;
import org.store.abonnement.application.dto.PaiementAbonnementResponse;
import org.store.abonnement.application.dto.RejectPaiementRequest;
import org.store.abonnement.application.dto.SubscriptionAmountBreakdown;
import org.store.abonnement.application.service.impl.PaiementAbonnementCreationContext;
import org.store.abonnement.application.service.impl.PaiementAbonnementServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.model.TypeAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.CouponDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.abonnement.domain.service.PromotionDomainService;
import org.store.abonnement.domain.service.UtilisationCouponDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.EntityException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.model.PieceJointe;
import org.store.common.service.IUploadFileService;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementAbonnementServiceImplTest {

    @Mock private PaiementAbonnementDomainService paiementAbonnementDomainService;
    @Mock private AbonnementDomainService abonnementDomainService;
    @Mock private PromotionDomainService promotionDomainService;
    @Mock private CouponDomainService couponDomainService;
    @Mock private UtilisationCouponDomainService utilisationCouponDomainService;
    @Mock private IUploadFileService uploadFileService;
    @Mock private SubscriptionAmountCalculator amountCalculator;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private PaiementAbonnementServiceImpl service;

    private UUID entrepriseId;
    private UUID abonnementId;
    private UUID paiementId;
    private Entreprise entreprise;
    private PlanAbonnement plan;
    private TypeAbonnement type;
    private Abonnement abonnement;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();
        paiementId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());
        plan.setPrix(new BigDecimal("19900"));

        type = new TypeAbonnement();
        type.setId(UUID.randomUUID());
        type.setDureeMois(12);

        abonnement = new Abonnement();
        abonnement.setId(abonnementId);
        abonnement.setEntreprise(entreprise);
        abonnement.setPlan(plan);
        abonnement.setTypeAbonnement(type);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
        abonnement.setActif(false);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null,
                "owner", "PROPRIETAIRE", List.of("SUBSCRIPTION_PAY", "SUBSCRIPTION_READ"));
    }

    private UserPrincipal admin() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "admin", "ADMIN", List.of("ADMIN_ACCESS", "SUBSCRIPTION_VALIDATE", "SUBSCRIPTION_READ"));
    }

    private SubscriptionAmountBreakdown sampleBreakdown() {
        return new SubscriptionAmountBreakdown(
                new BigDecimal("238800"), BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                new BigDecimal("238800"));
    }

    private MultipartFile validFile() {
        return new MockMultipartFile("file", "preuve.png", "image/png", new byte[]{1, 2, 3});
    }

    private PaiementAbonnement pendingPaiement() {
        PaiementAbonnement p = new PaiementAbonnement();
        p.setId(paiementId);
        p.setAbonnement(abonnement);
        p.setStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
        return p;
    }

    private PaiementAbonnementRequest sampleRequest() {
        return new PaiementAbonnementRequest("WAVE", "TXN-123", LocalDate.now());
    }

    @Test
    void create_should_persist_pending_payment() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnementId)).thenReturn(abonnement);
        when(paiementAbonnementDomainService.existsPendingForAbonnement(abonnementId)).thenReturn(false);
        when(promotionDomainService.findFirstActivePromotionForPlan(eq(plan.getId()), any(LocalDate.class)))
                .thenReturn(Optional.empty());
        when(utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId)).thenReturn(Optional.empty());
        when(amountCalculator.calculate(any(SubscriptionAmountInputs.class))).thenReturn(sampleBreakdown());
        when(uploadFileService.buildImage(any(MultipartFile.class))).thenReturn(new PieceJointe());
        when(paiementAbonnementDomainService.createPending(any(PaiementAbonnementCreationContext.class)))
                .thenReturn(pendingPaiement());

        PaiementAbonnementResponse response = service.create(abonnementId, sampleRequest(), validFile());

        assertThat(response.id()).isEqualTo(paiementId);
        assertThat(response.statut()).isEqualTo(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
    }

    @Test
    void create_should_throw_when_abonnement_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        abonnement.setEntreprise(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnementId)).thenReturn(abonnement);

        assertThatThrownBy(() -> service.create(abonnementId, sampleRequest(), validFile()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void create_should_throw_when_abonnement_not_pending() {
        abonnement.setStatut(AbonnementStatut.ACTIF);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnementId)).thenReturn(abonnement);

        assertThatThrownBy(() -> service.create(abonnementId, sampleRequest(), validFile()))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void create_should_throw_when_already_pending_payment() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(abonnementDomainService.findById(abonnementId)).thenReturn(abonnement);
        when(paiementAbonnementDomainService.existsPendingForAbonnement(abonnementId))
                .thenReturn(true);

        assertThatThrownBy(() -> service.create(abonnementId, sampleRequest(), validFile()))
                .isInstanceOf(BadArgumentException.class);

        verify(paiementAbonnementDomainService, never()).createPending(any(PaiementAbonnementCreationContext.class));
    }

    @Test
    void validate_should_activate_abonnement_and_set_dates() {
        PaiementAbonnement paiement = pendingPaiement();
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(abonnementDomainService.findLatestActifDateFin(entrepriseId, abonnement.getId())).thenReturn(Optional.empty());
        when(abonnementDomainService.activate(eq(abonnement), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    abonnement.setDateDebut(inv.getArgument(1));
                    abonnement.setDateFin(inv.getArgument(2));
                    abonnement.setActif(true);
                    abonnement.setStatut(AbonnementStatut.ACTIF);
                    return abonnement;
                });
        when(paiementAbonnementDomainService.markAsValide(paiement)).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.VALIDE);
            return paiement;
        });

        PaiementAbonnementResponse response = service.validate(paiementId);

        assertThat(abonnement.getStatut()).isEqualTo(AbonnementStatut.ACTIF);
        assertThat(abonnement.isActif()).isTrue();
        assertThat(abonnement.getDateDebut()).isEqualTo(LocalDate.now());
        assertThat(abonnement.getDateFin()).isEqualTo(LocalDate.now().plusMonths(12));
        assertThat(response.statut()).isEqualTo(StatutPaiementAbonnement.VALIDE);
    }

    @Test
    void validate_should_start_after_current_active_dateFin() {
        PaiementAbonnement paiement = pendingPaiement();

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(abonnementDomainService.findLatestActifDateFin(entrepriseId, abonnement.getId()))
                .thenReturn(Optional.of(LocalDate.of(2026, 12, 31)));
        when(abonnementDomainService.activate(eq(abonnement), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    abonnement.setDateDebut(inv.getArgument(1));
                    abonnement.setDateFin(inv.getArgument(2));
                    return abonnement;
                });
        when(paiementAbonnementDomainService.markAsValide(paiement)).thenReturn(paiement);

        service.validate(paiementId);

        assertThat(abonnement.getDateDebut()).isEqualTo(LocalDate.of(2027, 1, 1));
        assertThat(abonnement.getDateFin()).isEqualTo(LocalDate.of(2028, 1, 1));
    }

    @Test
    void validate_should_throw_when_payment_already_validated() {
        PaiementAbonnement paiement = pendingPaiement();
        paiement.setStatut(StatutPaiementAbonnement.VALIDE);
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.validate(paiementId))
                .isInstanceOf(BadArgumentException.class);

        verify(abonnementDomainService, never()).activate(any(), any(), any());
    }

    @Test
    void reject_should_release_coupon_and_mark_rejected() {
        PaiementAbonnement paiement = pendingPaiement();
        UUID couponId = UUID.randomUUID();
        Coupon coupon = new Coupon();
        coupon.setId(couponId);
        coupon.setNombreUtilisations(1);

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId)).thenReturn(Optional.of(couponId));
        when(couponDomainService.findById(couponId)).thenReturn(coupon);
        when(paiementAbonnementDomainService.markAsRejete(paiement, "Preuve illisible")).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.REJETE);
            paiement.setMotifRejet("Preuve illisible");
            return paiement;
        });

        service.reject(paiementId, new RejectPaiementRequest("Preuve illisible"));

        assertThat(paiement.getStatut()).isEqualTo(StatutPaiementAbonnement.REJETE);
        assertThat(paiement.getMotifRejet()).isEqualTo("Preuve illisible");
        verify(couponDomainService).decrementUsage(coupon);
        verify(utilisationCouponDomainService).deleteByAbonnementId(abonnementId);
    }

    @Test
    void reject_should_work_without_coupon() {
        PaiementAbonnement paiement = pendingPaiement();
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(utilisationCouponDomainService.findCouponIdByAbonnementId(abonnementId)).thenReturn(Optional.empty());
        when(paiementAbonnementDomainService.markAsRejete(paiement, "Montant incorrect")).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.REJETE);
            paiement.setMotifRejet("Montant incorrect");
            return paiement;
        });

        service.reject(paiementId, new RejectPaiementRequest("Montant incorrect"));

        assertThat(paiement.getStatut()).isEqualTo(StatutPaiementAbonnement.REJETE);
        verify(couponDomainService, never()).decrementUsage(any());
    }

    @Test
    void reject_should_throw_when_payment_already_rejected() {
        PaiementAbonnement paiement = pendingPaiement();
        paiement.setStatut(StatutPaiementAbonnement.REJETE);
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.reject(paiementId, new RejectPaiementRequest("x")))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findAll_should_force_entrepriseId_for_non_admin() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        PaiementAbonnementFilter filter = new PaiementAbonnementFilter(null, null, null, 0, 10);
        Page<PaiementAbonnementResponse> page = new PageImpl<>(List.of());
        when(paiementAbonnementDomainService.findResponses(any(PaiementAbonnementFilter.class))).thenReturn(page);

        service.findAll(filter);

        verify(paiementAbonnementDomainService).findResponses(
                org.mockito.ArgumentMatchers.argThat(f -> entrepriseId.equals(f.entrepriseId())));
    }

    @Test
    void findAll_should_keep_filter_for_admin() {
        when(currentUserService.getCurrent()).thenReturn(admin());
        PaiementAbonnementFilter filter = new PaiementAbonnementFilter(null, null, null, 0, 10);
        Page<PaiementAbonnementResponse> page = new PageImpl<>(List.of());
        when(paiementAbonnementDomainService.findResponses(filter)).thenReturn(page);

        service.findAll(filter);

        verify(paiementAbonnementDomainService).findResponses(filter);
    }

    @Test
    void findResponseById_should_throw_when_other_entreprise_for_non_admin() {
        PaiementAbonnement paiement = pendingPaiement();
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        paiement.getAbonnement().setEntreprise(other);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.findResponseById(paiementId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void getPreuve_should_throw_when_no_proof_attached() {
        PaiementAbonnement paiement = pendingPaiement();
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.getPreuve(paiementId))
                .isInstanceOf(EntityException.class);
    }
}
