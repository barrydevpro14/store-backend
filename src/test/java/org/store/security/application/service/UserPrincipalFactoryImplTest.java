package org.store.security.application.service;

import org.store.security.application.service.impl.UserPrincipalFactoryImpl;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.application.strategies.UserPrincipalContext;
import org.store.security.application.strategies.UserPrincipalContextStrategy;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Role;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.model.Utilisateur;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserPrincipalFactoryImplTest {

    @Mock
    private IPermissionsService permissionsService;

    @Mock
    private UserPrincipalContextStrategy proprietaireStrategy;

    @Mock
    private UserPrincipalContextStrategy employeStrategy;

    @Mock
    private UserPrincipalContextStrategy utilisateurStrategy;

    private UserPrincipalFactoryImpl factory;

    @BeforeEach
    void setUp() {
        lenient().doReturn(Proprietaire.class).when(proprietaireStrategy).targetType();
        lenient().doReturn(Employe.class).when(employeStrategy).targetType();
        lenient().doReturn(Utilisateur.class).when(utilisateurStrategy).targetType();
        factory = new UserPrincipalFactoryImpl(
                permissionsService,
                List.of(utilisateurStrategy, proprietaireStrategy, employeStrategy)
        );
    }

    @Test
    void should_dispatch_to_proprietaire_strategy_and_compose_principal() {
        UUID entrepriseId = UUID.randomUUID();
        UUID magasinId = UUID.randomUUID();
        Proprietaire proprietaire = new Proprietaire();
        Role role = roleWith("PROPRIETAIRE");
        Account account = accountWith("john.doe", proprietaire, role);
        when(proprietaireStrategy.resolve(any(Utilisateur.class)))
                .thenReturn(new UserPrincipalContext(entrepriseId, magasinId));
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of("PROPRIETAIRE_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.accountId()).isEqualTo(account.getId());
        assertThat(principal.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(principal.magasinId()).isEqualTo(magasinId);
        assertThat(principal.username()).isEqualTo("john.doe");
        assertThat(principal.role()).isEqualTo("PROPRIETAIRE");
        assertThat(principal.permissions()).containsExactly("PROPRIETAIRE_ACCESS");
    }

    @Test
    void should_dispatch_to_employe_strategy() {
        UUID entrepriseId = UUID.randomUUID();
        UUID magasinId = UUID.randomUUID();
        Employe employe = new Employe();
        Role role = roleWith("MANAGER");
        Account account = accountWith("emp", employe, role);
        when(employeStrategy.resolve(any(Utilisateur.class)))
                .thenReturn(new UserPrincipalContext(entrepriseId, magasinId));
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of("EMPLOYE_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(principal.magasinId()).isEqualTo(magasinId);
        assertThat(principal.role()).isEqualTo("MANAGER");
    }

    @Test
    void should_fallback_to_utilisateur_strategy_for_plain_utilisateur() {
        Utilisateur user = new Utilisateur();
        Role role = roleWith("ADMIN");
        Account account = accountWith("admin", user, role);
        when(utilisateurStrategy.resolve(any(Utilisateur.class)))
                .thenReturn(UserPrincipalContext.empty());
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of("ADMIN_ACCESS"));

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isNull();
        assertThat(principal.magasinId()).isNull();
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.permissions()).containsExactly("ADMIN_ACCESS");
    }

    @Test
    void should_build_principal_with_null_tenant_when_no_strategy_supports_user() {
        Role role = roleWith("ADMIN");
        Account account = accountWith("admin", null, role);
        when(permissionsService.findAllByRoleId(role.getId())).thenReturn(List.of());

        UserPrincipal principal = factory.build(account);

        assertThat(principal.entrepriseId()).isNull();
        assertThat(principal.magasinId()).isNull();
        assertThat(principal.role()).isEqualTo("ADMIN");
        assertThat(principal.permissions()).isEmpty();
    }

    private Account accountWith(String username, Utilisateur user, Role role) {
        Account a = new Account();
        a.setId(UUID.randomUUID());
        a.setUsername(username);
        a.setUser(user);
        a.setRole(role);
        return a;
    }

    private Role roleWith(String libelle) {
        Role role = new Role();
        role.setId(UUID.randomUUID());
        role.setLibelle(libelle);
        return role;
    }
}
