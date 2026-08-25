package org.store.plateforme.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.country.domain.model.Country;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.plateforme.application.dto.DepensePlateformeFilter;
import org.store.plateforme.application.dto.DepensePlateformeRequest;
import org.store.plateforme.application.dto.DepensePlateformeResponse;
import org.store.plateforme.application.dto.DepensePlateformeTotalResponse;
import org.store.plateforme.domain.model.CategoryDepensePlateforme;
import org.store.plateforme.domain.model.DepensePlateforme;
import org.store.plateforme.domain.repository.DepensePlateformeRepository;

import java.math.BigDecimal;

@Service
public class DepensePlateformeDomainService extends GlobalService<DepensePlateforme, DepensePlateformeRepository> {
    public DepensePlateformeDomainService(DepensePlateformeRepository repository) {
        super(repository);
    }

    /** Crée et persiste une dépense plateforme après résolution des FK par le service applicatif. */
    public DepensePlateforme create(DepensePlateformeRequest request, CategoryDepensePlateforme category, MoyenPaiement moyen, Country country) {
        DepensePlateforme depense = new DepensePlateforme();
        depense.setCategory(category);
        depense.setLibelle(request.libelle());
        depense.setDescription(request.description());
        depense.setDateDepense(request.dateDepense());
        depense.setMontant(request.montant());
        depense.setModePaiement(moyen);
        depense.setCountry(country);
        return save(depense);
    }

    public Page<DepensePlateformeResponse> findResponsesByFilter(DepensePlateformeFilter filter) {
        return repository.findResponsesByFilter(
                filter.categoryId(), filter.moyenPaiementId(), filter.countryId(), filter.actif(),
                filter.libelle(), LikePatternHelper.toLikePattern(filter.libelle()),
                filter.startDate(), filter.endDate(),
                filter.toPageable());
    }

    public DepensePlateformeTotalResponse computeTotal(DepensePlateformeFilter filter) {
        return repository.computeTotal(
                filter.categoryId(), filter.moyenPaiementId(), filter.countryId(),
                filter.libelle(), LikePatternHelper.toLikePattern(filter.libelle()),
                filter.startDate(), filter.endDate());
    }

    public BigDecimal sumByPeriod(String startDate, String endDate, java.util.UUID countryId) {
        return repository.sumByPeriod(startDate, endDate, countryId);
    }
}
