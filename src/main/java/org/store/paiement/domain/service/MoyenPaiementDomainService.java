package org.store.paiement.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.dto.DataSelect;
import org.store.common.exceptions.EntityException;
import org.store.common.tools.LikePatternHelper;
import org.store.paiement.application.dto.MoyenPaiementSelectFilter;
import org.store.paiement.domain.model.MoyenPaiement;
import org.store.paiement.domain.repository.MoyenPaiementRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Gère les opérations de domaine sur les moyens de paiement globaux.
 */
@Service
public class MoyenPaiementDomainService {

    private final MoyenPaiementRepository repository;

    public MoyenPaiementDomainService(MoyenPaiementRepository repository) {
        this.repository = repository;
    }

    public MoyenPaiement findById(UUID id) {
        return repository.findById(id)
                .orElseThrow(() -> new EntityException("moyenPaiement.notFound", id));
    }

    public Optional<MoyenPaiement> findByCode(String code) {
        return repository.findByCode(code);
    }

    public List<MoyenPaiement> findAllActifs() {
        return repository.findAllByActifTrue();
    }

    public List<MoyenPaiement> findAll() {
        return repository.findAll();
    }

    public MoyenPaiement save(MoyenPaiement moyenPaiement) {
        return repository.save(moyenPaiement);
    }

    public void delete(MoyenPaiement moyenPaiement) {
        repository.delete(moyenPaiement);
    }

    public Page<DataSelect> findSelectItems(MoyenPaiementSelectFilter filter) {
        String searchPattern = LikePatternHelper.toLikePattern(filter.searchTerm());

        return repository.findSelectItems(filter.countryId(), filter.searchTerm(), searchPattern, filter.toPageable());
    }
}
