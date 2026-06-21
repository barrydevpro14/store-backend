package org.store.security.application.service;

import org.store.security.application.dto.UserPrincipal;

public interface IJwtService {

    String generateToken(UserPrincipal principal);

    /**
     * Émet un token restreint aux seules permissions de renouvellement d'abonnement.
     * Utilisé quand l'abonnement d'un OWNER est expiré : il peut souscrire mais ne peut
     * pas accéder aux routes métier.
     */
    String generateRestrictedToken(UserPrincipal principal);

    boolean isTokenValid(String token);

    UserPrincipal extractUserPrincipal(String token);
}
