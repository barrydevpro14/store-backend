package org.store.vente.application.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.store.common.exceptions.ForbiddenException;
import org.store.common.service.ValidatorService;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.application.service.IMagasinService;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.service.ICurrentUserService;
import org.store.vente.application.dto.ClientFilter;
import org.store.vente.application.dto.ClientRequest;
import org.store.vente.application.dto.ClientResponse;
import org.store.vente.application.service.impl.ClientServiceImpl;
import org.store.vente.domain.model.Client;
import org.store.vente.domain.service.ClientDomainService;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ClientServiceImplTest {

    @Mock private ClientDomainService clientDomainService;
    @Mock private IMagasinService magasinService;
    @Mock private ICurrentUserService currentUserService;
    @Mock private ValidatorService validatorService;

    @InjectMocks
    private ClientServiceImpl service;

    private UUID entrepriseId;
    private UUID magasinId;
    private UUID clientId;
    private Entreprise entreprise;
    private Magasin magasin;

    @BeforeEach
    void setUp() {
        entrepriseId = UUID.randomUUID();
        magasinId = UUID.randomUUID();
        clientId = UUID.randomUUID();

        entreprise = new Entreprise();
        entreprise.setId(entrepriseId);

        magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);
    }

    private UserPrincipal proprietaire() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, null, "owner", null, null, "OWNER",
                List.of("CLIENT_CREATE", "CLIENT_READ", "CLIENT_UPDATE", "CLIENT_DELETE"));
    }

    private UserPrincipal vendeur() {
        return new UserPrincipal(UUID.randomUUID(), UUID.randomUUID(), entrepriseId, magasinId, "seller", null, null, "SELLER",
                List.of("CLIENT_CREATE", "CLIENT_READ"));
    }

    private Client sample(Magasin attachedMagasin) {
        Client client = new Client();
        client.setId(clientId);
        client.setNom("Diallo");
        client.setPrenom("Mamadou");
        client.setEmail("mamadou@example.com");
        client.setTelephone("+221770000001");
        client.setAdresse("Dakar");
        client.setMagasin(attachedMagasin);
        return client;
    }

    @Test
    void create_should_persist_after_magasin_access_check() {
        ClientRequest request = new ClientRequest("Diallo", "Mamadou", "mamadou@example.com",
                "+221770000001", "Dakar", magasinId);
        Client created = sample(magasin);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin)).thenReturn(magasin);
        when(clientDomainService.create(request, magasin)).thenReturn(created);

        ClientResponse response = service.create(request);

        assertThat(response.id()).isEqualTo(clientId);
        assertThat(response.nom()).isEqualTo("Diallo");
        assertThat(response.prenom()).isEqualTo("Mamadou");
    }

    @Test
    void create_should_propagate_forbidden_when_magasin_not_accessible() {
        ClientRequest request = new ClientRequest("Diallo", null, null, magasinId);

        when(magasinService.findById(magasinId)).thenReturn(magasin);
        when(magasinService.ensureAccessibleByCurrentUser(magasin))
                .thenThrow(new ForbiddenException("magasin.notOwned"));

        assertThatThrownBy(() -> service.create(request))
                .isInstanceOf(ForbiddenException.class);

        verify(clientDomainService, never()).create(any(), any());
    }

    @Test
    void findResponseById_should_return_when_employe_owns_magasin() {
        Client client = sample(magasin);
        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(client);

        ClientResponse response = service.findResponseById(clientId);

        assertThat(response.id()).isEqualTo(clientId);
        assertThat(response.nom()).isEqualTo("Diallo");
    }

    @Test
    void findResponseById_should_throw_when_employe_other_magasin() {
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(entreprise);
        Client foreignClient = sample(foreignMagasin);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(foreignClient);

        assertThatThrownBy(() -> service.findResponseById(clientId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findResponseById_should_return_when_proprietaire_owns_entreprise() {
        Client client = sample(magasin);
        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(clientDomainService.findById(clientId)).thenReturn(client);

        ClientResponse response = service.findResponseById(clientId);

        assertThat(response.id()).isEqualTo(clientId);
        assertThat(response.nom()).isEqualTo("Diallo");
    }

    @Test
    void findResponseById_should_throw_when_proprietaire_other_entreprise() {
        Entreprise otherEntreprise = new Entreprise();
        otherEntreprise.setId(UUID.randomUUID());
        Magasin otherMagasin = new Magasin();
        otherMagasin.setId(UUID.randomUUID());
        otherMagasin.setEntreprise(otherEntreprise);
        Client foreignClient = sample(otherMagasin);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(clientDomainService.findById(clientId)).thenReturn(foreignClient);

        assertThatThrownBy(() -> service.findResponseById(clientId))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void findAllForCurrentUser_should_scope_to_magasin_for_employe() {
        ClientFilter filter = new ClientFilter(null, null, null, 0, 10);
        ClientResponse item = new ClientResponse(clientId, "Diallo", "Mamadou", null, "+221770000001", null);
        Page<ClientResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findResponsesByMagasinId(magasinId, filter)).thenReturn(page);

        Page<ClientResponse> result = service.findAllForCurrentUser(filter);

        assertThat(result.getContent()).containsExactly(item);
        verify(clientDomainService, never()).findResponsesByEntrepriseId(any(), any());
    }

    @Test
    void findAllForCurrentUser_should_scope_to_entreprise_for_proprietaire() {
        ClientFilter filter = new ClientFilter(null, null, null, 0, 10);
        ClientResponse item = new ClientResponse(clientId, "Diallo", "Mamadou", null, "+221770000001", null);
        Page<ClientResponse> page = new PageImpl<>(List.of(item), PageRequest.of(0, 10), 1);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(clientDomainService.findResponsesByEntrepriseId(entrepriseId, filter)).thenReturn(page);

        Page<ClientResponse> result = service.findAllForCurrentUser(filter);

        assertThat(result.getContent()).containsExactly(item);
        verify(clientDomainService, never()).findResponsesByMagasinId(any(), any());
    }

    @Test
    void findAllForCurrentUser_should_forward_nom_and_prenom() {
        ClientFilter filter = new ClientFilter("Diallo", "Mama", null, null, 0, 10);
        Page<ClientResponse> emptyPage = new PageImpl<>(List.of(), PageRequest.of(0, 10), 0);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findResponsesByMagasinId(eq(magasinId), eq(filter))).thenReturn(emptyPage);

        service.findAllForCurrentUser(filter);

        verify(validatorService).validate(filter);
        verify(clientDomainService).findResponsesByMagasinId(magasinId, filter);
    }

    @Test
    void update_should_change_fields_same_magasin() {
        Client client = sample(magasin);
        ClientRequest request = new ClientRequest("Diop", "Awa", "awa@example.com",
                "771111111", "Thiès", magasinId);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(client);
        when(clientDomainService.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        ClientResponse response = service.update(clientId, request);

        assertThat(response.nom()).isEqualTo("Diop");
        assertThat(response.prenom()).isEqualTo("Awa");
        assertThat(response.adresse()).isEqualTo("Thiès");
        verify(magasinService, never()).findById(any());
    }

    @Test
    void update_should_check_new_magasin_when_changed() {
        Client client = sample(magasin);
        UUID newMagasinId = UUID.randomUUID();
        Magasin newMagasin = new Magasin();
        newMagasin.setId(newMagasinId);
        newMagasin.setEntreprise(entreprise);

        ClientRequest request = new ClientRequest("Diallo", null, null, newMagasinId);

        when(currentUserService.getCurrent()).thenReturn(proprietaire());
        when(clientDomainService.findById(clientId)).thenReturn(client);
        when(magasinService.findById(newMagasinId)).thenReturn(newMagasin);
        when(magasinService.ensureAccessibleByCurrentUser(newMagasin)).thenReturn(newMagasin);
        when(clientDomainService.save(any(Client.class))).thenAnswer(inv -> inv.getArgument(0));

        service.update(clientId, request);

        verify(magasinService).ensureAccessibleByCurrentUser(newMagasin);
        org.mockito.ArgumentCaptor<Client> captor = org.mockito.ArgumentCaptor.forClass(Client.class);
        verify(clientDomainService).save(captor.capture());
        assertThat(captor.getValue().getMagasin().getId()).isEqualTo(newMagasinId);
    }

    @Test
    void update_should_throw_when_other_scope() {
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(entreprise);
        Client foreignClient = sample(foreignMagasin);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(foreignClient);

        ClientRequest updateReq = new ClientRequest("x", null, null, magasinId);

        assertThatThrownBy(() -> service.update(clientId, updateReq))
                .isInstanceOf(ForbiddenException.class);

        verify(clientDomainService, never()).save(any());
    }

    @Test
    void delete_should_remove_when_accessible() {
        Client client = sample(magasin);
        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(client);

        service.delete(clientId);

        verify(clientDomainService).delete(client);
    }

    @Test
    void delete_should_throw_when_other_scope() {
        Magasin foreignMagasin = new Magasin();
        foreignMagasin.setId(UUID.randomUUID());
        foreignMagasin.setEntreprise(entreprise);
        Client foreignClient = sample(foreignMagasin);

        when(currentUserService.getCurrent()).thenReturn(vendeur());
        when(clientDomainService.findById(clientId)).thenReturn(foreignClient);

        assertThatThrownBy(() -> service.delete(clientId))
                .isInstanceOf(ForbiddenException.class);

        verify(clientDomainService, never()).delete(any(Client.class));
    }
}
