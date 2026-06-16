package org.store.entreprise.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
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

    public EntrepriseDomainService(EntrepriseRepository repository,
                                    CountryDomainService countryDomainService) {
        super(repository);
        this.countryDomainService = countryDomainService;
    }

    public Entreprise create(EntrepriseRequest entrepriseRequest, Proprietaire proprietaire) {
        Entreprise entreprise = new Entreprise();
        entreprise.setProprietaire(proprietaire);
        entreprise.setSigle(entrepriseRequest.sigle());
        entreprise.setRaisonSociale(entrepriseRequest.raisonSociale());
        entreprise.setNinea(entrepriseRequest.ninea());
        entreprise.setRccm(entrepriseRequest.rccm());
        entreprise.setAdresse(entrepriseRequest.adresse());

        if (entrepriseRequest.countryId() != null) {
            Country country = countryDomainService.findById(entrepriseRequest.countryId());
            entreprise.setCountry(country);
        }

        entreprise.setTrialUsed(true);
        entreprise.setActif(true);
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
                filter.startDate(), filter.endDate(),
                filter.toPageable());
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
