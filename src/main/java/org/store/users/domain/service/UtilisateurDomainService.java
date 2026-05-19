package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.UniqueResourceException;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.repository.UtilisateurRepository;

import java.util.UUID;

@Service
public class UtilisateurDomainService extends GlobalService<Utilisateur, UtilisateurRepository> {
    public UtilisateurDomainService(UtilisateurRepository repository) {
        super(repository);
    }

    /** Met a jour les champs Person (nom, prenom, email, telephone, adresse). Username/role/magasin geres ailleurs. */
    public Utilisateur update(Utilisateur utilisateur, UserProfileUpdateRequest request) {
        utilisateur.setNom(request.nom());
        utilisateur.setPrenom(request.prenom());
        utilisateur.setEmail(request.email());
        utilisateur.setTelephone(request.telephone());
        utilisateur.setAdresse(request.adresse());
        return save(utilisateur);
    }

    /** Remplace la photo de profil. orphanRemoval supprime automatiquement l'ancienne PieceJointe. */
    public Utilisateur setPhoto(Utilisateur utilisateur, PieceJointe photo) {
        utilisateur.setPhoto(photo);
        return save(utilisateur);
    }

    /** Supprime la photo de profil (orphanRemoval supprime la PieceJointe associee). */
    public Utilisateur clearPhoto(Utilisateur utilisateur) {
        utilisateur.setPhoto(null);
        return save(utilisateur);
    }

    /**
     * Verifie que l'email et le telephone ne sont pas deja pris par un autre
     * Person en base. A utiliser AVANT toute creation d'Utilisateur (inscription
     * proprietaire, creation employe). Leve `UniqueResourceException` separement
     * pour l'email puis le telephone : le frontend peut ainsi afficher l'erreur
     * sur le bon champ via `setError`.
     */
    public void ensureContactsAvailable(String email, String telephone) {
        if (repository.existsByEmail(email)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (repository.existsByTelephone(telephone)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }

    /**
     * Variante de {@link #ensureContactsAvailable} pour les updates : la ligne
     * d'identifiant `currentUserId` est exclue du check (l'utilisateur a le
     * droit de conserver son propre email / telephone inchanges).
     */
    public void ensureContactsAvailableForUpdate(String email, String telephone, UUID currentUserId) {
        if (repository.existsByEmailAndIdNot(email, currentUserId)) {
            throw new UniqueResourceException("utilisateur.email.alreadyExists", email);
        }
        if (repository.existsByTelephoneAndIdNot(telephone, currentUserId)) {
            throw new UniqueResourceException("utilisateur.telephone.alreadyExists", telephone);
        }
    }
}
