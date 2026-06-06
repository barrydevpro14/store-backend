package org.store.common.tools;

import org.junit.jupiter.api.Test;
import org.store.common.exceptions.ForbiddenException;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OwnershipHelperTest {

    @Test
    void ensureOwnership_should_return_entity_when_ids_match() {
        UUID entrepriseId = UUID.randomUUID();
        String entity = "some-entity";

        String result = OwnershipHelper.ensureOwnership(entity, entrepriseId, entrepriseId, "entity.notOwned");

        assertThat(result).isSameAs(entity);
    }

    @Test
    void ensureOwnership_should_throw_ForbiddenException_when_ids_differ() {
        UUID entityEntrepriseId = UUID.randomUUID();
        UUID currentUserEntrepriseId = UUID.randomUUID();
        String entity = "some-entity";

        assertThatThrownBy(() ->
                OwnershipHelper.ensureOwnership(entity, entityEntrepriseId, currentUserEntrepriseId, "entity.notOwned"))
                .isInstanceOf(ForbiddenException.class);
    }

    @Test
    void ensureOwnership_should_preserve_entity_type() {
        UUID entrepriseId = UUID.randomUUID();
        Integer entity = 42;

        Integer result = OwnershipHelper.ensureOwnership(entity, entrepriseId, entrepriseId, "entity.notOwned");

        assertThat(result).isEqualTo(42);
    }
}
