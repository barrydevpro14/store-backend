package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.store.abonnement.application.dto.*;
import org.store.abonnement.application.service.impl.PaiementAbonnementServiceImpl;
import org.store.abonnement.application.service.impl.SubscriptionAmountCalculator;
import org.store.abonnement.application.service.impl.SubscriptionAmountInputs;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.abonnement.domain.model.PlanAbonnement;
import org.store.abonnement.domain.service.AbonnementDomainService;
import org.store.abonnement.domain.service.PaiementAbonnementDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.ValidatorService;
import org.store.country.domain.model.Country;
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
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;
    @Mock private org.store.notification.application.service.INotificationEventPublisher notificationEventPublisher;
    @Mock private org.store.audit.application.service.IAuditEventPublisher auditEventPublisher;
    @Mock private IRevenuService revenuService;

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
        Country country = new Country();
        country.setId(UUID.randomUUID());
        entreprise.setCountry(country);

        plan = new PlanAbonnement();
        plan.setId(UUID.randomUUID());

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

    @Test
    void confirmPaiement_should_activate_abonnement_en_attente_with_today_plus_one_month() {
        PaiementAbonnement facture = factureGeneree();
        LocalDate datePaiement = LocalDate.now().minusDays(1);

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);
        when(abonnementDomainService.activate(eq(abonnement), any(LocalDate.class), any(LocalDate.class)))
                .thenAnswer(inv -> {
                    abonnement.setDateDebut(inv.getArgument(1));
                    abonnement.setDateFin(inv.getArgument(2));
                    abonnement.setStatut(AbonnementStatut.ACTIF);
                    return abonnement;
                });
        when(paiementAbonnementDomainService.markAsValide(facture, datePaiement)).thenAnswer(inv -> {
            facture.setDatePaiement(datePaiement);
            facture.setStatut(StatutPaiementAbonnement.VALIDE);
            return facture;
        });
        when(currentUserService.getCurrent()).thenReturn(admin());

        PaiementAbonnementResponse response = service.confirmPaiement(paiementId, datePaiement);

        assertThat(abonnement.getStatut()).isEqualTo(AbonnementStatut.ACTIF);
        assertThat(response.statut()).isEqualTo(StatutPaiementAbonnement.VALIDE);
        assertThat(response.datePaiement()).isEqualTo(datePaiement);
        verify(revenuService).record(any(RevenuRecordCommand.class));
    }

    @Test
    void confirmPaiement_should_extend_dateFin_when_abonnement_actif() {
        abonnement.setStatut(AbonnementStatut.ACTIF);
        abonnement.setPeriodicite(org.store.abonnement.domain.enums.PeriodiciteAbonnement.MENSUEL);
        abonnement.setDateFin(LocalDate.of(2026, 12, 31));
        PaiementAbonnement facture = factureGeneree();
        LocalDate datePaiement = LocalDate.now();

        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);
        when(abonnementDomainService.save(abonnement)).thenReturn(abonnement);
        when(paiementAbonnementDomainService.markAsValide(facture, datePaiement)).thenAnswer(inv -> {
            facture.setStatut(StatutPaiementAbonnement.VALIDE);
            return facture;
        });
        when(currentUserService.getCurrent()).thenReturn(admin());

        service.confirmPaiement(paiementId, datePaiement);

        assertThat(abonnement.getDateFin()).isEqualTo(LocalDate.of(2027, 1, 31));
        verify(revenuService).record(any(RevenuRecordCommand.class));
    }

    @Test
    void confirmPaiement_should_throw_when_facture_not_facture_generee() {
        PaiementAbonnement facture = factureGeneree();
        facture.setStatut(StatutPaiementAbonnement.VALIDE);
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);

        assertThatThrownBy(() -> service.confirmPaiement(paiementId, LocalDate.now()))
                .isInstanceOf(BadArgumentException.class);

        verify(abonnementDomainService, never()).activate(any(), any(), any());
    }

    @Test
    void findDetailsById_should_return_facture_with_preuves() {
        PaiementAbonnement facture = factureGeneree();
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);

        PaiementAbonnementDetailsResponse details = service.findDetailsById(paiementId);

        assertThat(details.id()).isEqualTo(paiementId);
        assertThat(details.preuves()).isEmpty();
    }

    @Test
    void findDetailsById_should_throw_when_other_entreprise() {
        PaiementAbonnement facture = factureGeneree();
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        facture.getAbonnement().setEntreprise(other);
        when(paiementAbonnementDomainService.findById(paiementId)).thenReturn(facture);

        assertThatThrownBy(() -> service.findDetailsById(paiementId))
                .isInstanceOf(org.store.common.exceptions.ForbiddenException.class);
    }

    @Test
    void findMyPending_should_return_details_when_facture_exists() {
        PaiementAbonnementDetailsResponse details = new PaiementAbonnementDetailsResponse(factureGeneree());
        when(paiementAbonnementDomainService.findCurrentUnpaidFactureByEntreprise(entrepriseId))
                .thenReturn(Optional.of(details));

        Optional<PaiementAbonnementDetailsResponse> result = service.findMyPending();

        assertThat(result).isPresent();
        assertThat(result.get().facture().id()).isEqualTo(paiementId);
    }

    @Test
    void findMyPending_should_return_empty_when_no_current_entreprise() {
        when(currentUserService.getCurrent()).thenReturn(admin());

        Optional<PaiementAbonnementDetailsResponse> result = service.findMyPending();

        assertThat(result).isEmpty();
    }

    @Test
    void findAll_should_scope_filter_for_non_admin() {
        PaiementAbonnementFilter filter = new PaiementAbonnementFilter(null, null, null, null, null, 0, 10);
        when(paiementAbonnementDomainService.findResponses(any())).thenReturn(new PageImpl<>(List.of()));

        service.findAll(filter);

        verify(validatorService).validate(filter);
    }
}
