package org.store.security.application.strategies;

import org.junit.jupiter.api.Test;
import org.store.entreprise.domain.model.Entreprise;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ProprietairePrincipalContextStrategyTest {

    private final ProprietairePrincipalContextStrategy strategy = new ProprietairePrincipalContextStrategy();

    @Test
    void target_type_should_be_proprietaire() {
        assertThat(strategy.targetType()).isEqualTo(Proprietaire.class);
    }

    @Test
    void resolve_should_return_entreprise_id_and_null_magasin_id() {
        UUID entrepriseId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setEntreprise(entreprise);

        UserPrincipalContext context = strategy.resolve(proprietaire);

        assertThat(context.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(context.magasinId()).isNull();
    }

    @Test
    void resolve_should_return_empty_when_entreprise_is_null() {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setEntreprise(null);

        UserPrincipalContext context = strategy.resolve(proprietaire);

        assertThat(context.entrepriseId()).isNull();
        assertThat(context.magasinId()).isNull();
    }
}
