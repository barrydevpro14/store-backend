package org.store.depense.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseParCategorieResponse;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.common.tools.DateHelper;
import org.store.depense.domain.repository.DepenseParCategorieProjection;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.repository.DepenseRepository;
import org.store.magasin.domain.model.Magasin;
import org.store.paiement.domain.model.MoyenPaiement;

import java.util.List;
import java.util.UUID;

@Service
public class DepenseDomainService extends GlobalService<Depense, DepenseRepository> {
    public DepenseDomainService(DepenseRepository repository) {
        super(repository);
    }

    /** Crée et persiste une dépense après résolution des FK Magasin + Category + MoyenPaiement par le service applicatif. */
    public Depense create(DepenseRequest depenseRequest, Magasin magasin, CategoryDepense category, MoyenPaiement moyen) {
        Depense depense = new Depense();
        depense.setMagasin(magasin);
        depense.setCategory(category);
        depense.setLibelle(depenseRequest.libelle());
        depense.setDescription(depenseRequest.description());
        depense.setDateDepense(depenseRequest.dateDepense());
        depense.setMontant(depenseRequest.montant());
        depense.setModePaiement(moyen);
        return save(depense);
    }

    public Page<DepenseResponse> findResponsesByFilter(DepenseFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    public DepenseTotalResponse computeTotal(DepenseFilter filter, UUID entrepriseId) {
        return repository.computeTotal(filter, entrepriseId);
    }

    /** Répartition des dépenses par catégorie, triée par montant décroissant. */
    public List<DepenseParCategorieResponse> computeByCategory(DepenseFilter filter, UUID entrepriseId) {
        String start = DateHelper.format(filter.fromDateSentinel());
        String end   = DateHelper.format(filter.toDateSentinel());
        List<DepenseParCategorieProjection> rows = repository.computeByCategory(filter.magasinId(), start, end, entrepriseId);
        return rows.stream()
                .map(p -> new DepenseParCategorieResponse(p.getCategoryId(), p.getCategoryNom(), p.getMontantTotal(), p.getNombreDepenses()))
                .toList();
    }
}
