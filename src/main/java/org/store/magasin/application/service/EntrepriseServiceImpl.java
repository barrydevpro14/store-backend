package org.store.magasin.application.service;

import org.springframework.stereotype.Service;
import org.store.magasin.application.dto.EntrepriseRequest;
import org.store.magasin.domain.model.Entreprise;
import org.store.magasin.domain.repository.EntrepriseRepository;
import org.store.users.domain.model.Proprietaire;

@Service
public class EntrepriseServiceImpl implements IEntrepriseService {

    private final EntrepriseRepository entrepriseRepository;

    public EntrepriseServiceImpl(EntrepriseRepository entrepriseRepository) {
        this.entrepriseRepository = entrepriseRepository;
    }

    @Override
    public Entreprise create(EntrepriseRequest info, Proprietaire proprietaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.setProprietaire(proprietaire);
        entreprise.setSigle(info.sigle());
        entreprise.setRaisonSociale(info.raisonSociale());
        entreprise.setNinea(info.ninea());
        entreprise.setRccm(info.rccm());
        entreprise.setAdresse(info.adresse());
        entreprise.setTrialUsed(true);
        return entrepriseRepository.save(entreprise);
    }
}
