package org.store.security.application.strategies;

import java.util.UUID;

public record UserPrincipalContext(UUID entrepriseId, UUID magasinId) {

    public static UserPrincipalContext empty() {
        return new UserPrincipalContext(null, null);
    }
}
