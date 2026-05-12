package org.store.security.application.strategies;

import org.junit.jupiter.api.Test;
import org.store.entreprise.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Employe;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EmployePrincipalContextStrategyTest {

    private final EmployePrincipalContextStrategy strategy = new EmployePrincipalContextStrategy();

    @Test
    void target_type_should_be_employe() {
        assertThat(strategy.targetType()).isEqualTo(Employe.class);
    }

    @Test
    void resolve_should_return_entreprise_id_and_magasin_id() {
        UUID entrepriseId = UUID.randomUUID();
        UUID magasinId = UUID.randomUUID();
        Entreprise entreprise = new Entreprise();
        entreprise.setId(entrepriseId);
        Magasin magasin = new Magasin();
        magasin.setId(magasinId);
        magasin.setEntreprise(entreprise);
        Employe employe = new Employe();
        employe.setMagasin(magasin);

        UserPrincipalContext context = strategy.resolve(employe);

        assertThat(context.entrepriseId()).isEqualTo(entrepriseId);
        assertThat(context.magasinId()).isEqualTo(magasinId);
    }

    @Test
    void resolve_should_return_empty_when_magasin_is_null() {
        Employe employe = new Employe();
        employe.setMagasin(null);

        UserPrincipalContext context = strategy.resolve(employe);

        assertThat(context.entrepriseId()).isNull();
        assertThat(context.magasinId()).isNull();
    }
}
