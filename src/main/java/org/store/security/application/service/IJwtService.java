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

    /**
     * Émet un token restreint avec scope "restricted_suspendu".
     * Utilisé quand l'abonnement est suspendu pour défaut de paiement : l'OWNER doit
     * payer la facture en retard, pas souscrire à un nouveau plan.
     */
    String generateSuspendedToken(UserPrincipal principal);

    /**
     * Émet un token restreint avec scope "restricted_inactif".
     * Utilisé quand l'abonnement a été désactivé par un admin : aucune action self-service possible.
     */
    String generateInactifToken(UserPrincipal principal);

    boolean isTokenValid(String token);

    UserPrincipal extractUserPrincipal(String token);
}
