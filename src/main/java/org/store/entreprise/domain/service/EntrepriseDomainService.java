package org.store.entreprise.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.entreprise.application.dto.EntrepriseFilter;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;
import org.store.users.domain.model.Proprietaire;

@Service
public class EntrepriseDomainService extends GlobalService<Entreprise, EntrepriseRepository> {
    public EntrepriseDomainService(EntrepriseRepository repository) {
        super(repository);
    }

    public Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.setProprietaire(proprietaire);
        entreprise.setSigle(entrepriseRequest.sigle());
        entreprise.setRaisonSociale(entrepriseRequest.raisonSociale());
        entreprise.setNinea(entrepriseRequest.ninea());
        entreprise.setRccm(entrepriseRequest.rccm());
        entreprise.setAdresse(entrepriseRequest.adresse());
        entreprise.setTrialUsed(true);
        entreprise.setActif(true);
        return save(entreprise);
    }

    /** Listing pagine filtre (sigle/raisonSociale/ninea/rccm LIKE insensitive, actif). */
    public Page<EntrepriseResponse> findResponsesByFilter(EntrepriseFilter filter) {
        return repository.findResponsesByFilter(filter, filter.toPageable());
    }

    /** Pose ou remplace le logo. orphanRemoval supprime auto l'ancienne PieceJointe. */
    public Entreprise setLogo(Entreprise entreprise, PieceJointe logo) {
        entreprise.setLogo(logo);
        return save(entreprise);
    }

    /** Supprime le logo (orphanRemoval supprime la PieceJointe). */
    public Entreprise clearLogo(Entreprise entreprise) {
        entreprise.setLogo(null);
        return save(entreprise);
    }
}
