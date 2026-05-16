package org.store.inventaire.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.inventaire.application.dto.BilanInventaireRequest;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.application.dto.LigneInventaireRequest;
import org.store.inventaire.application.dto.LigneInventaireResponse;
import org.store.inventaire.application.dto.RapportInventaireResponse;

import java.util.UUID;

public interface IInventaireService {

    InventaireResponse create(UUID magasinId);

    LigneInventaireResponse addLigne(UUID inventaireId, LigneInventaireRequest request);

    Page<LigneInventaireResponse> findLignes(UUID inventaireId, Pageable pageable);

    InventaireResponse passerEnBilan(UUID inventaireId, BilanInventaireRequest request);

    InventaireResponse cloturer(UUID inventaireId);

    InventaireResponse annuler(UUID inventaireId);

    Page<InventaireResponse> findAllByCurrentEntreprise(InventaireFilter filter);

    InventaireResponse findResponseById(UUID id);

    RapportInventaireResponse findRapportByInventaireId(UUID inventaireId);
}
