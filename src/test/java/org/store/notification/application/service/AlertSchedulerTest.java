package org.store.notification.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.abonnement.application.service.IAbonnementService;
import org.store.abonnement.application.service.IPaiementAbonnementService;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.achat.application.service.IFactureAchatService;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.FactureAchat;
import org.store.common.i18n.IMessageSourceService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.notification.application.event.AbonnementExpiringEvent;
import org.store.notification.application.event.FactureAchatOverdueEvent;
import org.store.notification.application.event.FactureClientOverdueEvent;
import org.store.notification.application.service.impl.AlertScheduler;
import org.store.notification.domain.enums.AlerteStatut;
import org.store.notification.domain.enums.AlerteType;
import org.store.vente.application.service.IFactureClientService;
import org.store.vente.domain.model.FactureClient;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.RETURNS_DEEP_STUBS;

@ExtendWith(MockitoExtension.class)
class AlertSchedulerTest {

    @Mock private IAbonnementService abonnementService;
    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private IFactureClientService factureClientService;
    @Mock private IFactureAchatService factureAchatService;
    @Mock private INotificationEventPublisher eventPublisher;
    @Mock private IAlertService alertService;
    @Mock private IMessageSourceService messageSourceService;

    @InjectMocks
    private AlertScheduler scheduler;

    @BeforeEach
    void setUp() {
        lenient().when(abonnementService.findExpiringOnDates(any())).thenReturn(List.of());
        lenient().when(paiementAbonnementService.findFacturesAbonnementDues(any())).thenReturn(List.of());
        lenient().when(factureClientService.findDueOnDates(any(), any())).thenReturn(List.of());
        lenient().when(factureAchatService.findDueOnDates(any(), any())).thenReturn(List.of());

        lenient().when(messageSourceService.getMessage(anyString())).thenReturn("");
        lenient().when(messageSourceService.getMessage(anyString(), any(Object[].class))).thenReturn("");
        lenient().when(messageSourceService.getMessage(anyString(), any(Object[].class), any(Locale.class))).thenReturn("");
    }

    @Test
    void runDailyAlertsAsync_should_skip_all_checks_when_no_data() {
        scheduler.runDailyAlertsAsync();

        verify(alertService, never()).create(any(), any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void checkAbonnementsExpiring_should_create_alert_and_publish_event() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        Abonnement abonnement = new Abonnement();
        abonnement.setId(UUID.randomUUID());
        abonnement.setEntreprise(entreprise);
        abonnement.setDateFin(LocalDate.now().plusDays(3));

        when(abonnementService.findExpiringOnDates(any())).thenReturn(List.of(abonnement));

        scheduler.runDailyAlertsAsync();

        verify(alertService).create(
                eq(AlerteType.ABONNEMENT_EXPIRING), eq(AlerteStatut.NOUVELLE),
                any(), any(), eq(entreprise.getId()), eq(null), eq(abonnement.getId()), eq(3));
        verify(eventPublisher).publishEvent(any(AbonnementExpiringEvent.class));
    }

    @Test
    void checkFacturesAbonnementDues_should_create_alert_for_due_invoice() {
        Entreprise entreprise = new Entreprise();
        entreprise.setId(UUID.randomUUID());

        Abonnement abonnement = new Abonnement();
        abonnement.setId(UUID.randomUUID());
        abonnement.setEntreprise(entreprise);

        PaiementAbonnement facture = new PaiementAbonnement();
        facture.setId(UUID.randomUUID());
        facture.setAbonnement(abonnement);
        facture.setMontantFinal(new BigDecimal("19900"));
        facture.setDateEcheance(LocalDate.now().plusDays(1));

        when(paiementAbonnementService.findFacturesAbonnementDues(any())).thenReturn(List.of(facture));

        scheduler.runDailyAlertsAsync();

        verify(alertService).create(
                eq(AlerteType.FACTURE_ABONNEMENT_DUE), eq(AlerteStatut.NOUVELLE),
                any(), any(), eq(entreprise.getId()), eq(null), eq(abonnement.getId()), eq(1));
    }

    @Test
    void checkFacturesClientOverdue_should_publish_event_for_overdue_invoice() {
        FactureClient facture = mock(FactureClient.class, RETURNS_DEEP_STUBS);
        UUID magasinId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        UUID commandeId = UUID.randomUUID();
        when(facture.getNumero()).thenReturn("FC-001");
        when(facture.getMontantTotal()).thenReturn(new BigDecimal("5000"));
        when(facture.getMontantPaye()).thenReturn(BigDecimal.ZERO);
        when(facture.getDateEcheance()).thenReturn(LocalDate.now().plusDays(5));
        when(facture.getCommande().getMagasin().getId()).thenReturn(magasinId);
        when(facture.getCommande().getMagasin().getEntreprise().getId()).thenReturn(entrepriseId);
        when(facture.getCommande().getId()).thenReturn(commandeId);

        when(factureClientService.findDueOnDates(any(), any())).thenReturn(List.of(facture));

        scheduler.runDailyAlertsAsync();

        verify(alertService).create(
                eq(AlerteType.FACTURE_VENTE_OVERDUE), eq(AlerteStatut.NOUVELLE),
                any(), any(), eq(entrepriseId), eq(magasinId), eq(commandeId), eq(5));
        verify(eventPublisher).publishEvent(any(FactureClientOverdueEvent.class));
    }

    @Test
    void checkFacturesAchatOverdue_should_publish_event_for_overdue_invoice() {
        FactureAchat facture = mock(FactureAchat.class, RETURNS_DEEP_STUBS);
        UUID magasinId = UUID.randomUUID();
        UUID entrepriseId = UUID.randomUUID();
        UUID commandeId = UUID.randomUUID();
        when(facture.getNumero()).thenReturn("FA-001");
        when(facture.getMontantTotal()).thenReturn(new BigDecimal("8000"));
        when(facture.getMontantPaye()).thenReturn(new BigDecimal("2000"));
        when(facture.getDateEcheance()).thenReturn(LocalDate.now());
        when(facture.getCommande().getMagasin().getId()).thenReturn(magasinId);
        when(facture.getCommande().getMagasin().getEntreprise().getId()).thenReturn(entrepriseId);
        when(facture.getCommande().getId()).thenReturn(commandeId);

        when(factureAchatService.findDueOnDates(any(), any())).thenReturn(List.of(facture));

        scheduler.runDailyAlertsAsync();

        verify(alertService).create(
                eq(AlerteType.FACTURE_ACHAT_OVERDUE), eq(AlerteStatut.NOUVELLE),
                any(), any(), eq(entrepriseId), eq(magasinId), eq(commandeId), eq(0));
        verify(eventPublisher).publishEvent(any(FactureAchatOverdueEvent.class));
    }
}
