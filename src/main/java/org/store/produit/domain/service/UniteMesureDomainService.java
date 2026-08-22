package org.store.produit.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.common.tools.LikePatternHelper;
import org.store.produit.application.dto.UniteMesureFilter;
import org.store.produit.application.dto.UniteMesureRequest;
import org.store.produit.application.dto.UniteMesureResponse;
import org.store.produit.domain.model.UniteMesure;
import org.store.produit.domain.repository.UniteMesureRepository;

import java.util.List;

@Service
public class UniteMesureDomainService extends GlobalService<UniteMesure, UniteMesureRepository> {

    public UniteMesureDomainService(UniteMesureRepository repository) {
        super(repository);
    }

    public UniteMesure create(UniteMesureRequest request) {
        UniteMesure unite = new UniteMesure();

        unite.setCode(request.code().toUpperCase());
        unite.setLibelle(request.libelle());
        unite.setSymbole(request.symbole());

        return save(unite);
    }

    public Page<UniteMesureResponse> findResponsesByFilter(UniteMesureFilter filter) {
        return repository.findResponsesByFilter(
                filter.libelle(),
                LikePatternHelper.toLikePattern(filter.libelle()),
                filter.code(),
                LikePatternHelper.toLikePattern(filter.code()),
                filter.toPageable()
        );
    }

    public List<UniteMesure> findAllOrdered() {
        return repository.findAllOrdered();
    }

    public UniteMesure findByCode(String code) {
        return repository.findByCode(code)
                .orElseThrow(() -> new org.store.common.exceptions.EntityException("uniteMesure.notFound"));
    }

    public java.util.Optional<UniteMesure> findByCodeOptional(String code) {
        return repository.findByCode(code);
    }

    public boolean existsByCode(String code) {
        return repository.existsByCode(code);
    }
}
