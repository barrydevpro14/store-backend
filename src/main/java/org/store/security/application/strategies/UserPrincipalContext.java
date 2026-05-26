package org.store.security.application.strategies;

import java.util.UUID;

public record UserPrincipalContext(UUID entrepriseId, UUID magasinId, String currency, String countryName) {

    public static UserPrincipalContext empty() {
        return new UserPrincipalContext(null, null, null, null);
    }
}
