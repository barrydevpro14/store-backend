package org.store.depense.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.depense.application.dto.DepenseFilter;
import org.store.depense.application.dto.DepenseRequest;
import org.store.depense.application.dto.DepenseResponse;
import org.store.depense.application.dto.DepenseTotalResponse;
import org.store.depense.domain.model.CategoryDepense;
import org.store.depense.domain.model.Depense;
import org.store.depense.domain.repository.DepenseRepository;
import org.store.magasin.domain.model.Magasin;

import java.util.UUID;

@Service
public class DepenseDomainService extends GlobalService<Depense, DepenseRepository> {
    public DepenseDomainService(DepenseRepository repository) {
        super(repository);
    }

    /** Crée et persiste une dépense après résolution des FK Magasin + Category par le service applicatif. */
    public Depense create(DepenseRequest depenseRequest, Magasin magasin, CategoryDepense category) {
        Depense depense = new Depense();
        depense.setMagasin(magasin);
        depense.setCategory(category);
        depense.setLibelle(depenseRequest.libelle());
        depense.setDescription(depenseRequest.description());
        depense.setDateDepense(depenseRequest.dateDepense());
        depense.setMontant(depenseRequest.montant());
        depense.setModePaiement(depenseRequest.modePaiement());
        return save(depense);
    }

    public Page<DepenseResponse> findResponsesByFilter(DepenseFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(filter, entrepriseId, filter.toPageable());
    }

    public DepenseTotalResponse computeTotal(DepenseFilter filter, UUID entrepriseId) {
        return repository.computeTotal(filter, entrepriseId);
    }
}
