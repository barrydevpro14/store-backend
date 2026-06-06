package org.store.common.tools;

import org.store.common.exceptions.ForbiddenException;

import java.util.UUID;

/** Shared guard for entity ownership checks — centralises the if/throw pattern duplicated across all service impls. */
public final class OwnershipHelper {

    private OwnershipHelper() {
    }

    /**
     * Returns entity unchanged if entityEntrepriseId matches currentUserEntrepriseId,
     * otherwise throws ForbiddenException with the given i18n key.
     */
    public static <T> T ensureOwnership(T entity, UUID entityEntrepriseId, UUID currentUserEntrepriseId, String errorKey) {
        if (!entityEntrepriseId.equals(currentUserEntrepriseId)) {
            throw new ForbiddenException(errorKey);
        }
        return entity;
    }
}
