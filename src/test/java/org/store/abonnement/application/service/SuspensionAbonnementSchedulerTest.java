package org.store.abonnement.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.store.abonnement.application.service.impl.SuspensionAbonnementScheduler;
import org.store.abonnement.domain.enums.AbonnementStatut;
import org.store.abonnement.domain.enums.StatutPaiementAbonnement;
import org.store.abonnement.domain.model.Abonnement;
import org.store.abonnement.domain.model.PaiementAbonnement;
import org.store.entreprise.domain.model.Entreprise;
import org.store.notification.application.event.AbonnementSuspenduEvent;
import org.store.property.SubscriptionProperties;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SuspensionAbonnementSchedulerTest {

    @Mock private IPaiementAbonnementService paiementAbonnementService;
    @Mock private IAbonnementService abonnementService;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SubscriptionProperties subscriptionProperties;

    @InjectMocks
    private SuspensionAbonnementScheduler scheduler;

    private UUID entrepriseId;
    private Abonnement abonnement;
    private PaiementAbonnement factureGeneree;
    private LocalDate today;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        today = LocalDate.now();

        when(subscriptionProperties.joursSuspension()).thenReturn(0);

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        abonnement = new Abonnement();
        abonnement.setId(UUID.randomUUID());
        abonnement.setEntreprise(entreprise);
        abonnement.setStatut(AbonnementStatut.ACTIF);

        factureGeneree = new PaiementAbonnement();
        factureGeneree.setId(UUID.randomUUID());
        factureGeneree.setAbonnement(abonnement);
        factureGeneree.setStatut(StatutPaiementAbonnement.FACTURE_GENEREE);
        factureGeneree.setDateEcheance(today.minusDays(1));
    }

    @Test
    void suspendrePourNonPaiement_should_suspend_overdue_abonnement() {
        when(paiementAbonnementService.findOverdueInvoices(today)).thenReturn(List.of(factureGeneree));

        scheduler.suspendrePourNonPaiement();

        verify(paiementAbonnementService).markAsEnRetard(factureGeneree);
        verify(abonnementService).suspend(abonnement);
        verify(eventPublisher).publishEvent(any(AbonnementSuspenduEvent.class));
    }

    @Test
    void suspendrePourNonPaiement_should_skip_when_no_overdue_invoice() {
        when(paiementAbonnementService.findOverdueInvoices(today)).thenReturn(List.of());

        scheduler.suspendrePourNonPaiement();

        verify(paiementAbonnementService, never()).markAsEnRetard(any());
        verify(abonnementService, never()).suspend(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    void suspendrePourNonPaiement_should_suspend_multiple_abonnements() {
        Entreprise autreEntreprise = new Entreprise();
        autreEntreprise.setId(UUID.randomUUID());

        Abonnement autreAbonnement = new Abonnement();
        autreAbonnement.setId(UUID.randomUUID());
        autreAbonnement.setEntreprise(autreEntreprise);
        autreAbonnement.setStatut(AbonnementStatut.ACTIF);

        PaiementAbonnement autreFacture = new PaiementAbonnement();
        autreFacture.setId(UUID.randomUUID());
        autreFacture.setAbonnement(autreAbonnement);
        autreFacture.setStatut(StatutPaiementAbonnement.FACTURE_GENEREE);
        autreFacture.setDateEcheance(today.minusDays(2));

        when(paiementAbonnementService.findOverdueInvoices(today))
                .thenReturn(List.of(factureGeneree, autreFacture));

        scheduler.suspendrePourNonPaiement();

        verify(paiementAbonnementService).markAsEnRetard(factureGeneree);
        verify(paiementAbonnementService).markAsEnRetard(autreFacture);
        verify(abonnementService).suspend(abonnement);
        verify(abonnementService).suspend(autreAbonnement);
    }
}
