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
import org.store.achat.application.dto.CommandeAchatFilter;
import org.store.achat.application.dto.CommandeAchatResponse;
import org.store.achat.application.service.impl.CommandeAchatServiceImpl;
import org.store.achat.domain.enums.CommandeAchatStatut;
import org.store.achat.domain.model.CommandeAchat;
import org.store.achat.domain.model.Fournisseur;
import org.store.achat.domain.service.CommandeAchatDomainService;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommandeAchatServiceImplTest {

    @Mock private CommandeAchatDomainService commandeAchatDomainService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private CommandeAchatServiceImpl service;

    private UUID commandeId;
    private UUID entrepriseId;
    private Entreprise entreprise;
    private Magasin magasin;
    private CommandeAchat commande;

    @BeforeEach
    void setUp() {
        commandeId = UUID.randomUUID();
        entrepriseId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(UUID.randomUUID());
        magasin.setEntreprise(entreprise);

        Fournisseur fournisseur = new Fournisseur();
        fournisseur.setId(UUID.randomUUID());
        fournisseur.setNom("Fournisseur Chine");
        fournisseur.setEntreprise(entreprise);

        commande = new CommandeAchat();
        commande.setId(commandeId);
        commande.setReference("CMD-AUTO");
        commande.setStatut(CommandeAchatStatut.RECEPTIONNEE);
        commande.setMagasin(magasin);
        commande.setFournisseur(fournisseur);
        commande.setLignes(List.of());
    }

    private UserPrincipal user() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", "OWNER", List.of("PURCHASE_READ"));
    }

    @Test
    void findResponseById_should_return_when_owned() {
        when(commandeAchatDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(user());

        CommandeAchatResponse response = service.findResponseById(commandeId);

        assertThat(response.id()).isEqualTo(commandeId);
        assertThat(response.reference()).isEqualTo("CMD-AUTO");
    }

    @Test
    void findResponseById_should_throw_forbidden_when_other_entreprise() {
        Entreprise autre = new Entreprise();
        autre.setId(UUID.randomUUID());
        magasin.setEntreprise(autre);

        when(commandeAchatDomainService.findById(commandeId)).thenReturn(commande);
        when(currentUserService.getCurrent()).thenReturn(user());

        assertThatThrownBy(() -> service.findResponseById(commandeId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllByCurrentEntreprise_should_validate_and_delegate() {
        CommandeAchatFilter filter = new CommandeAchatFilter(magasin.getId(), null, null, null, null, null, 0, 10);
        Page<CommandeAchatResponse> page = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(user());
        when(commandeAchatDomainService.findResponsesByFilter(eq(filter), eq(entrepriseId))).thenReturn(page);

        service.findAllByCurrentEntreprise(filter);

        verify(validatorService).validate(filter);
        verify(commandeAchatDomainService).findResponsesByFilter(eq(filter), eq(entrepriseId));
    }
}
