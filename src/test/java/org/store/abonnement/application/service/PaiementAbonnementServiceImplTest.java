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
import org.store.abonnement.application.service.impl.PaiementAbonnementServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.Coupon;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.application.service.ICouponService;
import org.store.abonnement.application.service.IUtilisationCouponService;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
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
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementAbonnementServiceImplTest {

    @Mock private PaiementAbonnementDomainService paiementAbonnementDomainService;
    @Mock private AbonnementDomainService abonnementDomainService;
    @Mock private ICouponService couponService;
    @Mock private IUtilisationCouponService utilisationCouponService;
    @Mock private IUploadFileService uploadFileService;
    @Mock private SubscriptionAmountCalculator amountCalculator;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private org.store.notification.application.service.INotificationEventPublisher notificationEventPublisher;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private org.store.paiement.application.service.IMoyenPaiementService moyenPaiementService;

    @InjectMocks
    private PaiementAbonnementServiceImpl service;

    private UUID entrepriseId;
    private UUID abonnementId;
    private UUID paiementId;
    private Entreprise entreprise;
    private PlanAbonnement plan;
    private Abonnement abonnement;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        abonnementId = UUID.randomUUID();
        paiementId = UUID.randomUUID();

        lenient().when(currentUserService.getCurrent()).thenReturn(proprietaire());

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());
        plan.setPrix(new BigDecimal("19900"));

        abonnement = new Abonnement();
        abonnement.setId(abonnementId);
        abonnement.setEntreprise(entreprise);
        abonnement.setPlanAbonnement(plan);
        abonnement.setStatut(AbonnementStatut.EN_ATTENTE);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null,
                "owner", null, null, "OWNER", List.of("SUBSCRIPTION_PAY", "SUBSCRIPTION_READ"));
    }

    private UserPrincipal admin() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), null,
                "admin", null, null, "ADMIN", List.of("ADMIN_ACCESS", "SUBSCRIPTION_VALIDATE", "SUBSCRIPTION_READ"));
    }

    private MultipartFile validFile() {
        return new MockMultipartFile("file", "preuve.png", "image/png", new byte[]{1, 2, 3});
    }

    private PaiementAbonnement factureGeneree() {
        PaiementAbonnement p = new PaiementAbonnement();
        p.setId(paiementId);
        p.setAbonnement(abonnement);
        p.setStatut(StatutPaiementAbonnement.FACTURE_GENEREE);
        p.setMontantAvantReduction(new BigDecimal("19900"));
        p.setReduction(BigDecimal.ZERO);
        p.setMontantFinal(new BigDecimal("19900"));
        return p;
    }

    private PaiementAbonnement pendingPaiement() {
        PaiementAbonnement p = new PaiementAbonnement();
        p.setId(paiementId);
        p.setAbonnement(abonnement);
        p.setStatut(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
        p.setMontantAvantReduction(new BigDecimal("19900"));
        p.setReduction(BigDecimal.ZERO);
        p.setMontantFinal(new BigDecimal("19900"));
        return p;
    }

    private PaiementAbonnementRequest sampleRequest() {
        return new PaiementAbonnementRequest(
                java.util.UUID.fromString("00000000-0000-0000-0000-000000000002"),
                "TXN-123",
                LocalDate.now());
    }

    @Test
    void payer_should_transition_facture_generee_to_en_attente_validation() {
        PaiementAbonnement facture = factureGeneree();

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);
        when(uploadFileService.buildImage(any(MultipartFile.class))).thenReturn(new PieceJointe());
        when(paiementAbonnementDomainService.save(any(PaiementAbonnement.class))).thenAnswer(inv -> inv.getArgument(0));

        PaiementAbonnementResponse response = service.payer(paiementId, sampleRequest(), validFile());

        assertThat(response.id()).isEqualTo(paiementId);
        assertThat(response.statut()).isEqualTo(StatutPaiementAbonnement.EN_ATTENTE_VALIDATION);
    }

    @Test
    void payer_should_throw_when_paiement_other_entreprise() {
        PaiementAbonnement facture = factureGeneree();
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        facture.getAbonnement().setEntreprise(other);

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);

        assertThatThrownBy(() -> service.payer(paiementId, sampleRequest(), validFile()))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void payer_should_throw_when_paiement_not_facture_generee() {
        PaiementAbonnement paiement = pendingPaiement();

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.payer(paiementId, sampleRequest(), validFile()))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void validate_should_activate_abonnement_en_attente_with_today_plus_one_month() {
        PaiementAbonnement paiement = pendingPaiement();
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(abonnementDomainService.activate(eq(abonnement), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    abonnement.setDateDebut(inv.getArgument(1));
                    abonnement.setDateFin(inv.getArgument(2));
                    abonnement.setStatut(AbonnementStatut.ACTIF);
                    return abonnement;
                });
        when(paiementAbonnementDomainService.markAsValide(paiement)).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.VALIDE);
            return paiement;
        });
        when(currentUserService.getCurrent()).thenReturn(admin());

        PaiementAbonnementResponse response = service.validate(paiementId);

        assertThat(abonnement.getStatut()).isEqualTo(AbonnementStatut.ACTIF);
        assertThat(abonnement.getDateDebut()).isEqualTo(LocalDate.now());
        assertThat(abonnement.getDateFin()).isEqualTo(LocalDate.now().plusMonths(1));
        assertThat(response.statut()).isEqualTo(StatutPaiementAbonnement.VALIDE);
    }

    @Test
    void validate_should_extend_dateFin_when_abonnement_actif() {
        abonnement.setStatut(AbonnementStatut.ACTIF);
        abonnement.setDateFin(LocalDate.of(2026, 12, 31));
        PaiementAbonnement paiement = pendingPaiement();

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(abonnementDomainService.save(abonnement)).thenReturn(abonnement);
        when(paiementAbonnementDomainService.markAsValide(paiement)).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.VALIDE);
            return paiement;
        });
        when(currentUserService.getCurrent()).thenReturn(admin());

        service.validate(paiementId);

        assertThat(abonnement.getDateFin()).isEqualTo(LocalDate.of(2027, 1, 31));
    }

    @Test
    void validate_should_throw_when_payment_not_en_attente_validation() {
        PaiementAbonnement paiement = factureGeneree();
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
        when(utilisationCouponService.findCouponIdByAbonnementId(abonnementId)).thenReturn(Optional.of(couponId));
        when(couponService.findById(couponId)).thenReturn(coupon);
        when(paiementAbonnementDomainService.markAsRejete(paiement, "Preuve illisible")).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.REJETE);
            paiement.setMotifRejet("Preuve illisible");
            return paiement;
        });

        service.reject(paiementId, new RejectPaiementRequest("Preuve illisible"));

        assertThat(paiement.getStatut()).isEqualTo(StatutPaiementAbonnement.REJETE);
        assertThat(paiement.getMotifRejet()).isEqualTo("Preuve illisible");
        verify(couponService).decrementUsage(coupon);
        verify(utilisationCouponService).deleteByAbonnementId(abonnementId);
    }

    @Test
    void reject_should_work_without_coupon() {
        PaiementAbonnement paiement = pendingPaiement();
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);
        when(utilisationCouponService.findCouponIdByAbonnementId(abonnementId)).thenReturn(Optional.empty());
        when(paiementAbonnementDomainService.markAsRejete(paiement, "Montant incorrect")).thenAnswer(inv -> {
            paiement.setStatut(StatutPaiementAbonnement.REJETE);
            paiement.setMotifRejet("Montant incorrect");
            return paiement;
        });

        service.reject(paiementId, new RejectPaiementRequest("Montant incorrect"));

        assertThat(paiement.getStatut()).isEqualTo(StatutPaiementAbonnement.REJETE);
        verify(couponService, never()).decrementUsage(any());
    }

    @Test
    void reject_should_throw_when_payment_not_en_attente_validation() {
        PaiementAbonnement paiement = pendingPaiement();
        paiement.setStatut(StatutPaiementAbonnement.REJETE);
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(paiement);

        assertThatThrownBy(() -> service.reject(paiementId, new RejectPaiementRequest("x")))
                .isInstanceOf(BadArgumentException.class);
    }

    @Test
    void findAll_should_force_entrepriseId_for_non_admin() {
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        PaiementAbonnementFilter filter = new PaiementAbonnementFilter(null, null, null, null, null, 0, 10);
        Page<PaiementAbonnementResponse> page = new PageImpl<>(List.of());
        when(paiementAbonnementDomainService.findResponses(any(PaiementAbonnementFilter.class))).thenReturn(page);

        service.findAll(filter);

        verify(paiementAbonnementDomainService).findResponses(
                org.mockito.ArgumentMatchers.argThat(f -> entrepriseId.equals(f.entrepriseId())));
    }

    @Test
    void findAll_should_keep_filter_for_admin() {
        when(currentUserService.getCurrent()).thenReturn(admin());
        PaiementAbonnementFilter filter = new PaiementAbonnementFilter(null, null, null, null, null, 0, 10);
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

    @Test
    void findMyPending_should_return_response_when_pending_exists() {
        PaiementAbonnementResponse response = pendingPaiement().getAbonnement() != null
                ? new PaiementAbonnementResponse(pendingPaiement()) : null;
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(paiementAbonnementDomainService.findPendingResponseByEntreprise(entrepriseId))
                .thenReturn(Optional.of(new PaiementAbonnementResponse(pendingPaiement())));

        Optional<PaiementAbonnementResponse> result = service.findMyPending();

        assertThat(result).isPresent();
        assertThat(result.get().id()).isEqualTo(paiementId);
    }

    @Test
    void findMyPending_should_return_empty_when_no_entreprise_id() {
        UserPrincipal noEntreprise = new UserPrincipal(
                UUID.randomUUID(), UUID.randomUUID(), null, null,
                "admin", null, null, "ADMIN", List.of("ADMIN_ACCESS"));
        when(currentUserService.getCurrent()).thenReturn(noEntreprise);

        Optional<PaiementAbonnementResponse> result = service.findMyPending();

        assertThat(result).isEmpty();
    }

    @Test
    void countByStatutAndCreatedBetween_should_parse_statut_and_delegate() {
        LocalDate debut = LocalDate.of(2026, 1,1);
        LocalDate fin =  LocalDate.of(2026, 12,31);
        when(paiementAbonnementDomainService.countByStatutAndCreatedBetween(
                StatutPaiementAbonnement.VALIDE, debut,fin)).thenReturn(7L);

        long result = service.countByStatutAndCreatedBetween("VALIDE", debut, fin);

        assertThat(result).isEqualTo(7L);
    }

    @Test
    void countByStatutAndCreatedBetween_should_pass_null_statut_when_blank() {
        when(paiementAbonnementDomainService.countByStatutAndCreatedBetween(null, null, null)).thenReturn(3L);

        long result = service.countByStatutAndCreatedBetween(null, null, null);

        assertThat(result).isEqualTo(3L);
    }

    @Test
    void findFacturesAbonnementDues_should_delegate_to_domain() {
        List<java.time.LocalDate> dates = List.of(LocalDate.now(), LocalDate.now().plusDays(1));
        PaiementAbonnement facture = factureGeneree();
        when(paiementAbonnementDomainService.findFacturesAbonnementDues(dates)).thenReturn(List.of(facture));

        List<PaiementAbonnement> result = service.findFacturesAbonnementDues(dates);

        assertThat(result).hasSize(1).containsExactly(facture);
    }
}
