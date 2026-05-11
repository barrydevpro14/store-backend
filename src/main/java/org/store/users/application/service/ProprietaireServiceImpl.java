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
    public Proprietaire create(UtilisateurRequest info, Account account) {
        Proprietaire proprietaire = new Proprietaire();
        proprietaire.setAccount(account);
        proprietaire.setNom(info.nom());
        proprietaire.setPrenom(info.prenom());
        proprietaire.setEmail(info.email());
        proprietaire.setTelephone(info.telephone());
        proprietaire.setAdresse(info.adresse());
        return proprietaireRepository.save(proprietaire);
    }
}
