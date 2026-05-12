package org.store.security.application.service;

import org.store.security.application.service.impl.UserPrincipalFactoryImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Proprietaire;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPrincipalFactoryImplTest {

    @Mock
    private IPermissionsService permissionsService;

    private UserPrincipalFactoryImpl factory;

    @BeforeEach
    void setUp() {
        factory = new UserPrincipalFactoryImpl(permissionsService);
    }

    @Test
    void should_build_principal_with_entreprise_and_first_magasin_for_proprietaire() {
        Magasin magasin = magasinWithId();
        Entreprise entreprise = entrepriseWith(List.of(magasin));
        magasin.setEntreprise(entreprise);

        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setEntreprise(entreprise);

        Role role = roleWith("PROPRIETAIRE");
        Account account = accountWith("john.doe", proprietaire, role);
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of("PROPRIETAIRE_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.userId()).isEqualTo(account.getId());
        assertThat(principal.entrepriseId()).isEqualTo(entreprise.getId());
        assertThat(principal.magasinId()).isEqualTo(magasin.getId());
        assertThat(principal.username()).isEqualTo("john.doe");
        assertThat(principal.role()).isEqualTo("PROPRIETAIRE");
        assertThat(principal.permissions()).containsExactly("PROPRIETAIRE_ACCESS");
    }

    @Test
    void should_build_principal_from_magasin_for_employe() {
        Magasin magasin = magasinWithId();
        Entreprise entreprise = entrepriseWith(null);
        magasin.setEntreprise(entreprise);

        Employe employe = new Employe();
        employe.setMagasin(magasin);

        Role role = roleWith("MANAGER");
        Account account = accountWith("emp", employe, role);
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of("EMPLOYE_ACCESS", "EMPLOYE_CREATE"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isEqualTo(entreprise.getId());
        assertThat(principal.magasinId()).isEqualTo(magasin.getId());
        assertThat(principal.role()).isEqualTo("MANAGER");
        assertThat(principal.permissions()).containsExactlyInAnyOrder("EMPLOYE_ACCESS", "EMPLOYE_CREATE");
    }

    @Test
    void should_build_principal_with_null_tenant_when_user_is_null() {
        Role role = roleWith("ADMIN");
        Account account = accountWith("admin", null, role);
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of());

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isNull();
        assertThat(principal.magasinId()).isNull();
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.permissions()).isEmpty();
    }

    private Account accountWith(String username, org.store.users.domain.model.Utilisateur user, Role role) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUsername(username);
        a.setUser(user);
        a.setRole(role);
        return a;
    }

    private Magasin magasinWithId() {
        Magasin m = new Magasin();
        m.setId(UUID.randomUUID());
        return m;
    }

    private Entreprise entrepriseWith(List<Magasin> magasins) {
        Entreprise e = new Entreprise();
        e.setId(UUID.randomUUID());
        e.setMagasins(magasins);
        return e;
    }

    private Role roleWith(String libelle) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setLibelle(libelle);
        return role;
    }
}
