package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.achat.domain.enums.StatutFacture;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteRequest;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.application.service.impl.PaiementVenteServiceImpl;
import org.store.vente.domain.model.CommandeVente;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

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
class PaiementVenteServiceImplTest {

    @Mock private PaiementVenteDomainService paiementVenteDomainService;
    @Mock private FactureClientDomainService factureClientDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private PaiementVenteServiceImpl service;

    private UUID entrepriseId;
    private UUID factureId;
    private FactureClient facture;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        factureId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        Magasin magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setEntreprise(entreprise);

        CommandeVente commande = new CommandeVente();
        commande.setId(UUID.randomUUID());
        commande.setMagasin(magasin);

        facture = new FactureClient();
        facture.setId(factureId);
        facture.setCommande(commande);
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(new BigDecimal("300.00"));
        facture.setStatut(StatutFacture.PARTIELLEMENT_PAYEE);
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, UUID.randomUUID(),
                "vendeur1", "VENDEUR", List.of("SALE_READ", "SALE_PAY"));
    }

    @Test
    void findByFactureId_should_delegate_with_currentEntreprise() {
        Pageable pageable = PageRequest.of(0, 10);
        PaiementVenteResponse paiement = new PaiementVenteResponse(
                UUID.randomUUID(), new BigDecimal("500.00"), LocalDate.of(2026, 5, 16),
                MoyenPaiement.CASH, factureId
        );
        Page<PaiementVenteResponse> page = new PageImpl<>(List.of(paiement), pageable, 1);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(paiementVenteDomainService.findResponsesByFactureId(factureId, entrepriseId, pageable))
                .thenReturn(page);

        Page<PaiementVenteResponse> result = service.findByFactureId(factureId, pageable);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().getFirst().montant()).isEqualByComparingTo(new BigDecimal("500.00"));
    }

    @Test
    void findByFactureId_should_return_empty_page_when_facture_belongs_to_other_entreprise() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<PaiementVenteResponse> emptyPage = new PageImpl<>(List.of(), pageable, 0);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(paiementVenteDomainService.findResponsesByFactureId(factureId, entrepriseId, pageable))
                .thenReturn(emptyPage);

        Page<PaiementVenteResponse> result = service.findByFactureId(factureId, pageable);

        assertThat(result.getContent()).isEmpty();
    }

    @Test
    void create_should_add_paiement_when_valid() {
        PaiementVenteRequest request = new PaiementVenteRequest(new BigDecimal("400.00"), MoyenPaiement.CASH.name(), null);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findById(factureId)).thenReturn(facture);
        PaiementVente paiement = new PaiementVente();
        paiement.setId(UUID.randomUUID());
        paiement.setFacture(facture);
        paiement.setMontant(new BigDecimal("400.00"));
        paiement.setMoyen(MoyenPaiement.CASH);
        paiement.setDatePaiement(LocalDate.now());
        when(paiementVenteDomainService.create(any(PaiementVenteCreate.class))).thenReturn(paiement);

        PaiementVenteResponse response = service.create(factureId, request);

        assertThat(response.montant()).isEqualByComparingTo(new BigDecimal("400.00"));
        assertThat(response.moyen()).isEqualTo(MoyenPaiement.CASH);
        ArgumentCaptor<PaiementVenteCreate> captor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        verify(paiementVenteDomainService).create(captor.capture());
        assertThat(captor.getValue().datePaiement()).isEqualTo(LocalDate.now());
        verify(factureClientDomainService).applyPaiement(facture, new BigDecimal("400.00"));
        verify(validatorService).validate(request);
    }

    @Test
    void create_should_use_datePaiement_from_request_when_provided() {
        LocalDate dateSaisie = LocalDate.of(2026, 5, 10);
        PaiementVenteRequest request = new PaiementVenteRequest(new BigDecimal("400.00"), MoyenPaiement.CASH.name(), dateSaisie);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findById(factureId)).thenReturn(facture);
        when(paiementVenteDomainService.create(any(PaiementVenteCreate.class))).thenReturn(new PaiementVente());

        service.create(factureId, request);

        ArgumentCaptor<PaiementVenteCreate> captor = ArgumentCaptor.forClass(PaiementVenteCreate.class);
        verify(paiementVenteDomainService).create(captor.capture());
        assertThat(captor.getValue().datePaiement()).isEqualTo(dateSaisie);
    }

    @Test
    void create_should_throw_forbidden_when_facture_belongs_to_other_entreprise() {
        Entreprise other = new Entreprise();
        other.setId(UUID.randomUUID());
        facture.getCommande().getMagasin().setEntreprise(other);

        PaiementVenteRequest request = new PaiementVenteRequest(new BigDecimal("100.00"), MoyenPaiement.CASH.name(), null);
        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findById(factureId)).thenReturn(facture);

        assertThatThrownBy(() -> service.create(factureId, request))
                .isInstanceOf(ForbiddenException.class);

        verify(paiementVenteDomainService, never()).create(any(PaiementVenteCreate.class));
        verify(factureClientDomainService, never()).applyPaiement(any(), any());
    }

    @Test
    void create_should_throw_when_facture_already_paid() {
        facture.setStatut(StatutFacture.PAYEE);
        facture.setMontantPaye(new BigDecimal("1000.00"));
        PaiementVenteRequest request = new PaiementVenteRequest(new BigDecimal("100.00"), MoyenPaiement.CASH.name(), null);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findById(factureId)).thenReturn(facture);

        assertThatThrownBy(() -> service.create(factureId, request))
                .isInstanceOf(BadArgumentException.class);

        verify(paiementVenteDomainService, never()).create(any(PaiementVenteCreate.class));
        verify(factureClientDomainService, never()).applyPaiement(any(), any());
    }

    @Test
    void create_should_throw_when_amount_exceeds_remaining() {
        // facture : montantTotal=1000, montantPaye=300 -> restant=700
        PaiementVenteRequest request = new PaiementVenteRequest(new BigDecimal("800.00"), MoyenPaiement.CASH.name(), null);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(factureClientDomainService.findById(factureId)).thenReturn(facture);

        assertThatThrownBy(() -> service.create(factureId, request))
                .isInstanceOf(BadArgumentException.class);

        verify(paiementVenteDomainService, never()).create(any(PaiementVenteCreate.class));
        verify(factureClientDomainService, never()).applyPaiement(any(), any());
    }
}
