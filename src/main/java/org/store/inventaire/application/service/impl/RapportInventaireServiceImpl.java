package org.store.inventaire.application.service.impl;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.application.service.IRapportInventaireService;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.service.RapportInventaireDomainService;

import java.util.Optional;
import java.util.UUID;

/**
 * Délègue la gestion du rapport d'inventaire au domain service.
 */
@Service
@Transactional(readOnly = true)
public class RapportInventaireServiceImpl implements IRapportInventaireService {

    private final RapportInventaireDomainService rapportInventaireDomainService;

    public RapportInventaireServiceImpl(RapportInventaireDomainService rapportInventaireDomainService) {
        this.rapportInventaireDomainService = rapportInventaireDomainService;
    }

    @Override
    @Transactional
    public void create(Inventaire inventaire, RapportInventaireCommand command) {
        rapportInventaireDomainService.create(inventaire, command);
    }

    @Override
    public Optional<RapportInventaireResponse> findResponseByInventaireId(UUID inventaireId, UUID entrepriseId) {
        return rapportInventaireDomainService.findResponseByInventaireId(inventaireId, entrepriseId);
    }
}
