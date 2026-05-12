package org.store.entreprise.application.service;

import org.springframework.stereotype.Service;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.service.EntrepriseDomainService;
import org.store.users.domain.model.Proprietaire;

import java.util.UUID;

@Service
public class EntrepriseServiceImpl implements IEntrepriseService {

    private final EntrepriseDomainService entrepriseDomainService;

    public EntrepriseServiceImpl(EntrepriseDomainService entrepriseDomainService) {
        this.entrepriseDomainService = entrepriseDomainService;
    }

    @Override
    public Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.setProprietaire(proprietaire);
        entreprise.setSigle(entrepriseRequest.sigle());
        entreprise.setRaisonSociale(entrepriseRequest.raisonSociale());
        entreprise.setNinea(entrepriseRequest.ninea());
        entreprise.setRccm(entrepriseRequest.rccm());
        entreprise.setAdresse(entrepriseRequest.adresse());
        entreprise.setTrialUsed(true);
        return entrepriseDomainService.save(entreprise);
    }

    @Override
    public Entreprise findById(UUID id) {
        return entrepriseDomainService.findById(id);
    }
}
