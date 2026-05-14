package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.achat.application.dto.PaiementAchatCreate;
import org.store.achat.application.dto.PaiementAchatRequest;
import org.store.achat.application.service.impl.PaiementAchatServiceImpl;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.achat.domain.service.PaiementAchatDomainService;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaiementAchatServiceImplTest {

    @Mock private FactureAchatDomainService factureAchatDomainService;
    @Mock private PaiementAchatDomainService paiementAchatDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private PaiementAchatServiceImpl service;

    private UUID factureId;
    private UUID entrepriseId;
    private FactureAchat facture;

    @BeforeEach
    void setUp() {
        factureId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        Magasin magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setEntreprise(entreprise);

        CommandeAchat commande = new CommandeAchat();
        commande.setId(UUID.randomUUID());
        commande.setMagasin(magasin);

        facture = new FactureAchat();
        facture.setId(factureId);
        facture.setCommande(commande);
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    private UserPrincipal user() {
        return new UserPrincipal(UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("PURCHASE_PAY"));
    }

    @Test
    void create_should_apply_payment_and_update_facture() {
        PaiementAchatRequest req = new PaiementAchatRequest(new BigDecimal("400.00"), LocalDate.of(2026, 5, 15), MoyenPaiement.CASH);
        PaiementAchat paiement = new PaiementAchat();
        paiement.setId(UUID.randomUUID());
        paiement.setFacture(facture);
        paiement.setMontant(new BigDecimal("400.00"));

        when(factureAchatDomainService.findById(factureId)).thenReturn(facture);
        when(currentUserService.getCurrent()).thenReturn(user());
        when(paiementAchatDomainService.create(any(PaiementAchatCreate.class))).thenReturn(paiement);
        when(factureAchatDomainService.applyPaiement(facture, new BigDecimal("400.00"))).thenReturn(facture);

        service.create(factureId, req);

        verify(validatorService).validate(req);
        verify(paiementAchatDomainService).create(any(PaiementAchatCreate.class));
        verify(factureAchatDomainService).applyPaiement(facture, new BigDecimal("400.00"));
    }

    @Test
    void create_should_throw_when_overpaiement() {
        facture.setMontantPaye(new BigDecimal("800.00"));
        PaiementAchatRequest req = new PaiementAchatRequest(new BigDecimal("500.00"), LocalDate.now(), MoyenPaiement.CASH);

        when(factureAchatDomainService.findById(factureId)).thenReturn(facture);
        when(currentUserService.getCurrent()).thenReturn(user());

        assertThatThrownBy(() -> service.create(factureId, req))
                .isInstanceOf(BadArgumentException.class);

        verify(paiementAchatDomainService, never()).create(any(PaiementAchatCreate.class));
        verify(factureAchatDomainService, never()).applyPaiement(any(), any());
    }

    @Test
    void create_should_throw_forbidden_when_facture_belongs_to_other_entreprise() {
        Entreprise autreEntreprise = new Entreprise();
        autreEntreprise.setId(UUID.randomUUID());
        Magasin autreMagasin = new Magasin();
        autreMagasin.setEntreprise(autreEntreprise);
        facture.getCommande().setMagasin(autreMagasin);

        PaiementAchatRequest req = new PaiementAchatRequest(new BigDecimal("100.00"), LocalDate.now(), MoyenPaiement.CASH);

        when(factureAchatDomainService.findById(factureId)).thenReturn(facture);
        when(currentUserService.getCurrent()).thenReturn(user());

        assertThatThrownBy(() -> service.create(factureId, req))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void applyPaiement_should_transition_statut_correctly() {
        FactureAchatDomainService realService = new FactureAchatDomainService(null) {
            @Override
            public org.store.achat.domain.model.FactureAchat save(org.store.achat.domain.model.FactureAchat f) {
                return f;
            }
        };

        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setMontantTotal(new BigDecimal("1000.00"));

        realService.applyPaiement(facture, new BigDecimal("400.00"));
        assertThat(facture.getStatut()).isEqualTo(StatutFacture.PARTIELLEMENT_PAYEE);
        assertThat(facture.getMontantPaye()).isEqualByComparingTo(new BigDecimal("400.00"));

        realService.applyPaiement(facture, new BigDecimal("600.00"));
        assertThat(facture.getStatut()).isEqualTo(StatutFacture.PAYEE);
        assertThat(facture.getMontantPaye()).isEqualByComparingTo(new BigDecimal("1000.00"));
    }
}
