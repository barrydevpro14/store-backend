package org.store.security.application.strategies;

import org.springframework.stereotype.Component;
import org.store.country.domain.model.Country;
import org.store.magasin.domain.model.Magasin;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Utilisateur;

@Component
public class EmployePrincipalContextStrategy implements UserPrincipalContextStrategy {

    @Override
    public Class<? extends Utilisateur> targetType() {
        return Employe.class;
    }

    @Override
    public UserPrincipalContext resolve(Utilisateur user) {
        Employe employe = (Employe) user;
        Magasin magasin = employe.getMagasin();
        if (magasin == null) {
            return UserPrincipalContext.empty();
        }
        var entreprise = magasin.getEntreprise();
        Country country = entreprise != null ? entreprise.getCountry() : null;
        String currency = country != null ? country.getCurrency() : null;
        String countryName = country != null ? country.getName() : null;
        return new UserPrincipalContext(
                entreprise != null ? entreprise.getId() : null,
                magasin.getId(),
                currency,
                countryName
        );
    }
}
