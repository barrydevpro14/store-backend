package org.store.achat.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.achat.application.dto.FactureAchatEcheanceFilter;
import org.store.achat.application.dto.FactureAchatFilter;
import org.store.achat.application.dto.FactureAchatResponse;
import org.store.achat.application.service.impl.FactureAchatServiceImpl;
import org.store.achat.domain.enums.StatutFacture;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.FactureAchat;
import org.store.achat.domain.service.FactureAchatDomainService;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FactureAchatServiceImplTest {

    @Mock private FactureAchatDomainService factureAchatDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private FactureAchatServiceImpl service;

    private UUID factureId;
    private UUID entrepriseId;
    private UUID magasinId;
    private FactureAchat facture;

    @BeforeEach
    void setUp() {
        factureId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();

        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        Magasin magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);

        CommandeAchat commande = new CommandeAchat();
        commande.setId(UUID.randomUUID());
        commande.setMagasin(magasin);

        facture = new FactureAchat();
        facture.setId(factureId);
        facture.setCommande(commande);
        facture.setNumero("FAC-001");
        facture.setMontantTotal(new BigDecimal("1000.00"));
        facture.setMontantPaye(BigDecimal.ZERO);
        facture.setStatut(StatutFacture.NON_PAYEE);
    }

    private UserPrincipal user() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", "PROPRIETAIRE", List.of("PURCHASE_READ"));
    }

    @Test
    void findResponseById_should_return_when_owned() {
        when(factureAchatDomainService.findById(factureId)).thenReturn(facture);
        when(currentUserService.getCurrent()).thenReturn(user());

        FactureAchatResponse response = service.findResponseById(factureId);

        assertThat(response.id()).isEqualTo(factureId);
        assertThat(response.numero()).isEqualTo("FAC-001");
    }

    @Test
    void findResponseById_should_throw_forbidden_when_other_entreprise() {
        Entreprise autre = new Entreprise();
        autre.setId(UUID.randomUUID());
        facture.getCommande().getMagasin().setEntreprise(autre);

        when(factureAchatDomainService.findById(factureId)).thenReturn(facture);
        when(currentUserService.getCurrent()).thenReturn(user());

        assertThatThrownBy(() -> service.findResponseById(factureId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findEcheances_should_validate_and_delegate() {
        FactureAchatEcheanceFilter filter = new FactureAchatEcheanceFilter(magasinId, "2026-05-01", "2026-05-31", 0, 10);
        Page<FactureAchatResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(factureAchatDomainService.findEcheances(eq(filter), eq(entrepriseId))).thenReturn(page);

        service.findEcheances(filter);

        verify(validatorService).validate(filter);
        verify(factureAchatDomainService).findEcheances(eq(filter), eq(entrepriseId));
    }
}
