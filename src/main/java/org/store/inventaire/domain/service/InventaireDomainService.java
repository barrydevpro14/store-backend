package org.store.inventaire.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.inventaire.application.dto.InventaireFilter;
import org.store.inventaire.application.dto.InventaireResponse;
import org.store.inventaire.domain.enums.InventaireStatut;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.repository.InventaireRepository;
import org.store.magasin.domain.model.Magasin;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
public class InventaireDomainService extends GlobalService<Inventaire, InventaireRepository> {
    public InventaireDomainService(InventaireRepository repository) {
        super(repository);
    }

    /** Retourne true si ce magasin possède déjà un inventaire non terminal (EN_COURS ou BILAN). */
    public boolean hasActiveInventaire(UUID magasinId) {
        return repository.existsByMagasinIdAndStatutIn(
                magasinId,
                List.of(InventaireStatut.EN_COURS, InventaireStatut.BILAN)
        );
    }

    /** Retourne l'inventaire actif (EN_COURS ou BILAN) du magasin, ou empty si aucun. */
    public java.util.Optional<org.store.inventaire.application.dto.InventaireResponse> findActive(UUID magasinId) {
        return repository.findActiveByMagasinId(
                        magasinId,
                        List.of(InventaireStatut.EN_COURS, InventaireStatut.BILAN))
                .stream().findFirst();
    }

    /** Crée un inventaire au statut EN_COURS pour un magasin et une date donnée. */
    public Inventaire create(Magasin magasin, LocalDate date) {
        Inventaire inventaire = new Inventaire();
        inventaire.setMagasin(magasin);
        inventaire.setDate(date);
        inventaire.setStatut(InventaireStatut.EN_COURS);
        return save(inventaire);
    }

    /** Transition de statut. Pose dateValidation = now() lors du passage en CLOTURE. */
    public Inventaire transitionStatut(Inventaire inventaire, InventaireStatut nouveauStatut) {
        inventaire.setStatut(nouveauStatut);
        if (nouveauStatut == InventaireStatut.CLOTURE) {
            inventaire.setDateValidation(LocalDateTime.now());
        }
        return save(inventaire);
    }

    /** Listing paginé filtré scopé entreprise. */
    public Page<InventaireResponse> findResponsesByFilter(InventaireFilter filter, UUID entrepriseId) {
        return repository.findResponsesByFilter(
                entrepriseId,
                filter.magasinId(),
                filter.statutAsEnum(),
                filter.startDate(),
                filter.endDate(),
                filter.toPageable());
    }

    /** Détail projeté JPQL, scopé entreprise. */
    public Optional<InventaireResponse> findResponseById(UUID id, UUID entrepriseId) {
        return repository.findResponseById(id, entrepriseId);
    }
}
