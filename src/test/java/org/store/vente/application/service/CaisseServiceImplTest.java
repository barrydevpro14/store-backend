package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.CaisseResumeResponse;
import org.store.vente.application.dto.PaiementParMoyenResponse;
import org.store.vente.application.dto.TopProduitResponse;
import org.store.vente.application.dto.TopProduitsFilter;
import org.store.vente.application.dto.VenteParVendeurResponse;
import org.store.vente.application.service.impl.CaisseServiceImpl;
import org.store.vente.domain.service.CommandeVenteDomainService;
import org.store.vente.domain.service.FactureClientDomainService;
import org.store.vente.domain.service.LigneCommandeVenteDomainService;
import org.store.vente.domain.service.PaiementVenteDomainService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CaisseServiceImplTest {

    @Mock private CommandeVenteDomainService commandeVenteDomainService;
    @Mock private FactureClientDomainService factureClientDomainService;
    @Mock private LigneCommandeVenteDomainService ligneCommandeVenteDomainService;
    @Mock private PaiementVenteDomainService paiementVenteDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private CaisseServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);
    }

    private UserPrincipal currentUser() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId,
                "vendeur1", "VENDEUR", List.of("SALE_READ"));
    }

    @Test
    void getResume_should_aggregate_6_queries_and_return_response_with_ventilations() {
        CaisseResumeFilter filter = new CaisseResumeFilter(magasinId, "2026-05-16");
        List<PaiementParMoyenResponse> paiementsParMoyen = List.of(
                new PaiementParMoyenResponse(MoyenPaiement.CASH, new BigDecimal("60000.00"), 18L),
                new PaiementParMoyenResponse(MoyenPaiement.WAVE, new BigDecimal("38500.00"), 6L)
        );
        List<VenteParVendeurResponse> ventesParVendeur = List.of(
                new VenteParVendeurResponse(UUID.randomUUID(), "Diop Awa", 15L, new BigDecimal("85000.00")),
                new VenteParVendeurResponse(UUID.randomUUID(), "Ba Modou", 12L, new BigDecimal("60000.00"))
        );

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(commandeVenteDomainService.countCommandesForCaisse(filter, entrepriseId)).thenReturn(27L);
        when(commandeVenteDomainService.sumQuantiteProduitsForCaisse(filter, entrepriseId)).thenReturn(312L);
        when(factureClientDomainService.sumMontantCommandesForCaisse(filter, entrepriseId)).thenReturn(new BigDecimal("145000.00"));
        when(paiementVenteDomainService.sumPaiementsForCaisse(filter, entrepriseId)).thenReturn(new BigDecimal("98500.00"));
        when(paiementVenteDomainService.ventilationParMoyenForCaisse(filter, entrepriseId)).thenReturn(paiementsParMoyen);
        when(commandeVenteDomainService.ventilationParVendeurForCaisse(filter, entrepriseId)).thenReturn(ventesParVendeur);

        CaisseResumeResponse result = service.getResume(filter);

        assertThat(result.magasinId()).isEqualTo(magasinId);
        assertThat(result.date()).isEqualTo(LocalDate.of(2026, 5, 16));
        assertThat(result.nombreCommandes()).isEqualTo(27L);
        assertThat(result.nombreProduits()).isEqualTo(312L);
        assertThat(result.totalCommandes()).isEqualByComparingTo(new BigDecimal("145000.00"));
        assertThat(result.totalPaiements()).isEqualByComparingTo(new BigDecimal("98500.00"));
        assertThat(result.paiementsParMoyen()).hasSize(2);
        assertThat(result.paiementsParMoyen().get(0).moyen()).isEqualTo(MoyenPaiement.CASH);
        assertThat(result.paiementsParMoyen().get(0).total()).isEqualByComparingTo(new BigDecimal("60000.00"));
        assertThat(result.paiementsParMoyen().get(0).nombre()).isEqualTo(18L);
        assertThat(result.ventesParVendeur()).hasSize(2);
        assertThat(result.ventesParVendeur().get(0).nomComplet()).isEqualTo("Diop Awa");
        assertThat(result.ventesParVendeur().get(0).nombreCommandes()).isEqualTo(15L);
        verify(validatorService).validate(filter);
    }

    @Test
    void getResume_should_propagate_forbidden_when_magasin_not_accessible() {
        CaisseResumeFilter filter = new CaisseResumeFilter(magasinId, "2026-05-16");

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.getResume(filter))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findTopProduits_should_delegate_and_return_list() {
        TopProduitsFilter filter = new TopProduitsFilter(magasinId, "2026-05-16", 3);
        List<TopProduitResponse> top = List.of(
                new TopProduitResponse(UUID.randomUUID(), "Clou 10mm", "CL-10", 250L, new BigDecimal("12500.00")),
                new TopProduitResponse(UUID.randomUUID(), "Vis M6", "VS-M6", 180L, new BigDecimal("9000.00")),
                new TopProduitResponse(UUID.randomUUID(), "Boulon 8mm", "BL-08", 95L, new BigDecimal("4750.00"))
        );

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(ligneCommandeVenteDomainService.findTopProduitsForCaisse(filter, entrepriseId)).thenReturn(top);

        List<TopProduitResponse> result = service.findTopProduits(filter);

        assertThat(result).hasSize(3);
        assertThat(result.get(0).quantiteVendue()).isEqualTo(250L);
        assertThat(result.get(0).nom()).isEqualTo("Clou 10mm");
        verify(validatorService).validate(filter);
    }

    @Test
    void findTopProduits_should_use_today_when_date_null() {
        TopProduitsFilter filter = new TopProduitsFilter(magasinId, null, 3);

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(ligneCommandeVenteDomainService.findTopProduitsForCaisse(filter, entrepriseId)).thenReturn(List.of());

        service.findTopProduits(filter);

        assertThat(filter.effectiveDate()).isEqualTo(LocalDate.now());
    }

    @Test
    void getResume_should_return_zero_values_when_no_activity_for_day() {
        CaisseResumeFilter filter = new CaisseResumeFilter(magasinId, "2026-05-16");

        when(currentUserService.getCurrent()).thenReturn(currentUser());
        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(commandeVenteDomainService.countCommandesForCaisse(filter, entrepriseId)).thenReturn(0L);
        when(commandeVenteDomainService.sumQuantiteProduitsForCaisse(filter, entrepriseId)).thenReturn(0L);
        when(factureClientDomainService.sumMontantCommandesForCaisse(filter, entrepriseId)).thenReturn(BigDecimal.ZERO);
        when(paiementVenteDomainService.sumPaiementsForCaisse(filter, entrepriseId)).thenReturn(BigDecimal.ZERO);
        when(paiementVenteDomainService.ventilationParMoyenForCaisse(filter, entrepriseId)).thenReturn(List.of());
        when(commandeVenteDomainService.ventilationParVendeurForCaisse(filter, entrepriseId)).thenReturn(List.of());

        CaisseResumeResponse result = service.getResume(filter);

        assertThat(result.nombreCommandes()).isZero();
        assertThat(result.nombreProduits()).isZero();
        assertThat(result.totalCommandes()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.totalPaiements()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(result.paiementsParMoyen()).isEmpty();
        assertThat(result.ventesParVendeur()).isEmpty();
    }
}
