package org.store.security.application.strategies;

import org.junit.jupiter.api.Test;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.model.Utilisateur;

import static org.assertj.core.api.Assertions.assertThat;

class UtilisateurPrincipalContextStrategyTest {

    private final UtilisateurPrincipalContextStrategy strategy = new UtilisateurPrincipalContextStrategy();

    @Test
    void target_type_should_be_utilisateur() {
        assertThat(strategy.targetType()).isEqualTo(Utilisateur.class);
    }

    @Test
    void resolve_should_return_empty_context() {
        Utilisateur user = new Proprietaire();

        UserPrincipalContext context = strategy.resolve(user);

        assertThat(context.entrepriseId()).isNull();
        assertThat(context.magasinId()).isNull();
    }
}
