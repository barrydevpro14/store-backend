package org.store.security.application.service;

import org.junit.jupiter.api.Test;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Proprietaire;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class UserPrincipalFactoryImplTest {

    private final UserPrincipalFactoryImpl factory = new UserPrincipalFactoryImpl();

    @Test
    void should_build_principal_with_entreprise_and_first_magasin_for_proprietaire() {
        Magasin magasin = magasinWithId();
        Entreprise entreprise = entrepriseWith(List.of(magasin));
        magasin.setEntreprise(entreprise);

        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setEntreprise(entreprise);

        Account account = accountWith("john.doe", proprietaire, roleWith("PROPRIETAIRE_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.userId()).isEqualTo(account.getId());
        assertThat(principal.entrepriseId()).isEqualTo(entreprise.getId());
        assertThat(principal.magasinId()).isEqualTo(magasin.getId());
        assertThat(principal.username()).isEqualTo("john.doe");
        assertThat(principal.permissions()).containsExactly("PROPRIETAIRE_ACCESS");
    }

    @Test
    void should_build_principal_from_magasin_for_employe() {
        Magasin magasin = magasinWithId();
        Entreprise entreprise = entrepriseWith(null);
        magasin.setEntreprise(entreprise);

        Employe employe = new Employe();
        employe.setMagasin(magasin);

        Account account = accountWith("emp", employe, roleWith("EMPLOYE_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isEqualTo(entreprise.getId());
        assertThat(principal.magasinId()).isEqualTo(magasin.getId());
        assertThat(principal.permissions()).containsExactly("EMPLOYE_ACCESS");
    }

    @Test
    void should_build_principal_with_null_tenant_when_user_is_null() {
        Account account = accountWith("admin", null, roleWith());

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isNull();
        assertThat(principal.magasinId()).isNull();
        assertThat(principal.permissions()).isEmpty();
    }

    @Test
    void should_return_empty_permissions_when_role_is_null() {
        Account account = accountWith("admin", null, null);

        UserPrincipal principal = factory.build(account);

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

    private Role roleWith(String... codes) {
        Role role = new Role();
        Set<Permissions> permissions = new LinkedHashSet<>();
        for (String code : codes) {
            Permissions p = new Permissions();
            p.setCode(code);
            permissions.add(p);
        }
        role.setPermissions(permissions);
        return role;
    }
}
