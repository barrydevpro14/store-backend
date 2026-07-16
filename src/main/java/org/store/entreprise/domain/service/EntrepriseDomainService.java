package org.store.entreprise.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.activite.domain.service.ActiviteEconomiqueDomainService;
import org.store.common.model.PieceJointe;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.country.domain.model.Country;
import org.store.country.domain.service.CountryDomainService;
import org.store.entreprise.application.dto.EntrepriseFilter;
import org.store.entreprise.application.dto.EntrepriseRequest;
import org.store.entreprise.application.dto.EntrepriseResponse;
import org.store.entreprise.domain.model.Entreprise;
import org.store.entreprise.domain.repository.EntrepriseRepository;
import org.store.users.domain.model.Proprietaire;

@Service
public class EntrepriseDomainService extends GlobalService<Entreprise, EntrepriseRepository> {

    private final CountryDomainService countryDomainService;
    private final ActiviteEconomiqueDomainService activiteEconomiqueDomainService;

    public EntrepriseDomainService(EntrepriseRepository repository,
                                   CountryDomainService countryDomainService,
                                   ActiviteEconomiqueDomainService activiteEconomiqueDomainService) {
        super(repository);
        this.countryDomainService = countryDomainService;
        this.activiteEconomiqueDomainService = activiteEconomiqueDomainService;
    }

    public Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.setProprietaire(proprietaire);
        entreprise.setSigle(entrepriseRequest.sigle());
        entreprise.setRaisonSociale(entrepriseRequest.raisonSociale());
        entreprise.setNinea(entrepriseRequest.ninea());
        entreprise.setRccm(entrepriseRequest.rccm());
        entreprise.setAdresse(entrepriseRequest.adresse());
        entreprise.setTelephone(entrepriseRequest.telephone());
        entreprise.setCountry(countryDomainService.findById(entrepriseRequest.countryId()));
        entreprise.setTrialUsed(true);
        entreprise.setActif(true);
        applyActivite(entreprise, entrepriseRequest);
        return save(entreprise);
    }

    /** Listing pagine filtre (sigle/raisonSociale/ninea/rccm LIKE insensitive, actif). */
    public Page<EntrepriseResponse> findResponsesByFilter(EntrepriseFilter filter) {
        return repository.findResponsesByFilter(
                filter.sigle(), LikePatternHelper.toLikePattern(filter.sigle()),
                filter.raisonSociale(), LikePatternHelper.toLikePattern(filter.raisonSociale()),
                filter.ninea(), LikePatternHelper.toLikePattern(filter.ninea()),
                filter.rccm(), LikePatternHelper.toLikePattern(filter.rccm()),
                filter.actif(),
                filter.activiteEconomiqueId(),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public Entreprise update(Entreprise entreprise, EntrepriseRequest request) {
        entreprise.setSigle(request.sigle());
        entreprise.setRaisonSociale(request.raisonSociale());
        entreprise.setNinea(request.ninea());
        entreprise.setRccm(request.rccm());
        entreprise.setAdresse(request.adresse());
        entreprise.setTelephone(request.telephone());
        entreprise.setCountry(countryDomainService.findById(request.countryId()));
        applyActivite(entreprise, request);
        return save(entreprise);
    }

    /** Résout et applique l'activité économique depuis la requête. */
    private void applyActivite(Entreprise entreprise, EntrepriseRequest request) {
        entreprise.setActiviteEconomique(
                activiteEconomiqueDomainService.findById(request.activiteEconomiqueId()));
    }

    public Entreprise setLogo(Entreprise entreprise, PieceJointe pieceJointe) {
        entreprise.setLogo(pieceJointe);
        return save(entreprise);
    }

    public Entreprise clearLogo(Entreprise entreprise) {
        entreprise.setLogo(null);
        return save(entreprise);
    }

    public long countByActif(boolean actif) {
        return repository.countByActif(actif);
    }
}
