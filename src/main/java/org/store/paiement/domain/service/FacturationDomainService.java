package org.store.paiement.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.exceptions.BadArgumentException;
import org.store.common.service.GlobalService;
import org.store.country.domain.model.Country;
import org.store.paiement.application.dto.FacturationRequest;
import org.store.paiement.domain.model.Facturation;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.FacturationRepository;

import java.util.UUID;

@Service
public class FacturationDomainService extends GlobalService<Facturation, FacturationRepository> {

    public FacturationDomainService(FacturationRepository repository) {
        super(repository);
    }

    public Facturation create(FacturationRequest request, MoyenPaiement moyenPaiement, Country pays) {
        Facturation facturation = new Facturation();
        facturation.setMoyenPaiement(moyenPaiement);
        facturation.setPays(pays);
        facturation.setNumeroFacturation(request.numeroFacturation());
        facturation.setActif(true);
        return save(facturation);
    }

    public void ensureUniqueMoyenPaysPair(UUID moyenPaiementId, UUID paysId, UUID excludeId) {
        if (repository.existsByMoyenAndPays(moyenPaiementId, paysId, excludeId)) {
            throw new BadArgumentException("facturation.alreadyExists");
        }
    }
}
