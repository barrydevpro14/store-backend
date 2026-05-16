package org.store.users.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.users.application.dto.UserProfileUpdateRequest;
import org.store.users.domain.model.Utilisateur;
import org.store.users.domain.repository.UtilisateurRepository;

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
}
