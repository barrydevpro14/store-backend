package org.store.security.application.service;

import org.springframework.stereotype.Service;
import org.store.magasin.domain.model.Entreprise;
import org.store.magasin.domain.model.Magasin;
import org.store.security.application.dto.UserPrincipal;
import org.store.security.domain.model.Account;
import org.store.security.domain.model.Permissions;
import org.store.security.domain.model.Role;
import org.store.users.domain.model.Employe;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.model.Utilisateur;

import java.util.List;
import java.util.UUID;

@Service
public class UserPrincipalFactoryImpl implements IUserPrincipalFactory {

    @Override
    public UserPrincipal build(Account account) {
        UUID entrepriseId = null;
        UUID magasinId = null;
        Utilisateur user = account.getUser();
        if (user instanceof Proprietaire p) {
            Entreprise entreprise = p.getEntreprise();
            if (entreprise != null) {
                entrepriseId = entreprise.getId();
                magasinId = firstMagasinId(entreprise);
            }
        } else if (user instanceof Employe e) {
            Magasin magasin = e.getMagasin();
            if (magasin != null) {
                magasinId = magasin.getId();
                if (magasin.getEntreprise() != null) {
                    entrepriseId = magasin.getEntreprise().getId();
                }
            }
        }
        return new UserPrincipal(
                account.getId(),
                entrepriseId,
                magasinId,
                account.getUsername(),
                extractPermissions(account.getRole())
        );
    }

    private UUID firstMagasinId(Entreprise entreprise) {
        List<Magasin> magasins = entreprise.getMagasins();
        if (magasins == null || magasins.isEmpty()) {
            return null;
        }
        return magasins.get(0).getId();
    }

    private List<String> extractPermissions(Role role) {
        if (role == null || role.getPermissions() == null) {
            return List.of();
        }
        return role.getPermissions().stream()
                .map(Permissions::getCode)
                .filter(code -> code != null && !code.isBlank())
                .toList();
    }
}
