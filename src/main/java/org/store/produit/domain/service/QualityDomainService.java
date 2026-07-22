package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.entreprise.domain.model.Entreprise;
import org.store.produit.application.dto.QualityFilter;
import org.store.produit.application.dto.QualityRequest;
import org.store.produit.application.dto.QualityResponse;
import org.store.produit.application.dto.QualitySummaryResponse;
import org.store.produit.domain.model.Quality;
import org.store.produit.domain.repository.QualityRepository;

import java.util.Optional;
import java.util.UUID;

@Service
public class QualityDomainService extends GlobalService<Quality, QualityRepository> {
    public QualityDomainService(QualityRepository repository) {
        super(repository);
    }

    public Quality create(QualityRequest qualityRequest, Entreprise entreprise) {
        Quality quality = new Quality();
        quality.setLibelle(qualityRequest.libelle());
        quality.setDescription(qualityRequest.description());
        quality.setEntreprise(entreprise);
        return save(quality);
    }

    public Page<QualityResponse> findResponsesByFilter(QualityFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(entrepriseId, filter.libelle(), LikePatternHelper.toLikePattern(filter.libelle()), filter.startDate(), filter.endDate(), filter.toPageable());
    }

    public Page<QualitySummaryResponse> searchSummaries(UUID entrepriseId, String q, Pageable pageable) {
        return repository.searchSummaries(entrepriseId, LikePatternHelper.toLikePattern(q), pageable);
    }

    public Optional<Quality> findByLibelleAndEntrepriseId(String libelle, UUID entrepriseId) {
        return repository.findByLibelleAndEntrepriseId(libelle, entrepriseId);
    }

    public boolean existsByLibelleAndEntrepriseId(String libelle, UUID entrepriseId) {
        return repository.existsByLibelleAndEntrepriseId(libelle, entrepriseId);
    }
}
