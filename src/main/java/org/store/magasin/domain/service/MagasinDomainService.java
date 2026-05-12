package org.store.magasin.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.magasin.application.dto.MagasinResponse;
import org.store.magasin.domain.model.Magasin;
import org.store.magasin.domain.repository.MagasinRepository;

import java.util.UUID;

@Service
public class MagasinDomainService extends GlobalService<Magasin, MagasinRepository> {
    public MagasinDomainService(MagasinRepository repository) {
        super(repository);
    }

    public Page<MagasinResponse> findResponsesByEntrepriseId(UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByEntrepriseId(entrepriseId, pageable);
    }
}
