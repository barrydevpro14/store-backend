package org.store.security.application.strategies;

import org.springframework.stereotype.Component;
import org.store.country.domain.model.Country;
import org.store.entreprise.domain.model.Entreprise;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.model.Utilisateur;

@Component
public class ProprietairePrincipalContextStrategy implements UserPrincipalContextStrategy {

    @Override
    public Class<? extends Utilisateur> targetType() {
        return Proprietaire.class;
    }

    @Override
    public UserPrincipalContext resolve(Utilisateur user) {
        Proprietaire proprietaire = (Proprietaire) user;
        Entreprise entreprise = proprietaire.getEntreprise();
        if (entreprise == null) {
            return UserPrincipalContext.empty();
        }

        Country country = entreprise.getCountry();
        String currency = country != null ? country.getCurrency() : null;
        String countryName = country != null ? country.getName() : null;
        return new UserPrincipalContext(entreprise.getId(), null, currency, countryName);
    }
}
