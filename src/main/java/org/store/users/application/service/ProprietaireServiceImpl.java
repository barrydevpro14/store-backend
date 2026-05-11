package org.store.users.application.service;

import org.springframework.stereotype.Service;
import org.store.security.domain.model.Account;
import org.store.users.application.dto.UtilisateurRequest;
import org.store.users.domain.model.Proprietaire;
import org.store.users.domain.repository.ProprietaireRepository;

@Service
public class ProprietaireServiceImpl implements IProprietaireService {

    private final ProprietaireRepository proprietaireRepository;

    public ProprietaireServiceImpl(ProprietaireRepository proprietaireRepository) {
        this.proprietaireRepository = proprietaireRepository;
    }

    @Override
    public Proprietaire create(UtilisateurRequest utilisateurRequest, Account account) {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setAccount(account);
        proprietaire.setNom(utilisateurRequest.nom());
        proprietaire.setPrenom(utilisateurRequest.prenom());
        proprietaire.setEmail(utilisateurRequest.email());
        proprietaire.setTelephone(utilisateurRequest.telephone());
        proprietaire.setAdresse(utilisateurRequest.adresse());
        return proprietaireRepository.save(proprietaire);
    }
}
