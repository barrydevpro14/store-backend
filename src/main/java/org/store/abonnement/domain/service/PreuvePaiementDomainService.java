package org.store.abonnement.domain.service;

import org.springframework.stereotype.Service;
import org.store.abonnement.domain.enums.StatutPreuvePaiement;
import org.store.abonnement.domain.model.PreuvePaiement;
import org.store.abonnement.domain.repository.PreuvePaiementRepository;
import org.store.common.service.GlobalService;

import java.util.List;
import java.util.UUID;

@Service
public class PreuvePaiementDomainService extends GlobalService<PreuvePaiement, PreuvePaiementRepository> {
    public PreuvePaiementDomainService(PreuvePaiementRepository repository) {
        super(repository);
    }

    public boolean existsPendingForFacture(UUID paiementAbonnementId) {
        return repository.existsByPaiementAbonnementIdAndStatut(paiementAbonnementId, StatutPreuvePaiement.EN_ATTENTE_VALIDATION);
    }

    public List<PreuvePaiement> findByFactureId(UUID paiementAbonnementId) {
        return repository.findByPaiementAbonnementIdOrderByCreatedAtDesc(paiementAbonnementId);
    }

    public PreuvePaiement markAsValidee(PreuvePaiement preuve) {
        preuve.setStatut(StatutPreuvePaiement.VALIDEE);
        return save(preuve);
    }

    public PreuvePaiement markAsRejetee(PreuvePaiement preuve, String motifRejet) {
        preuve.setStatut(StatutPreuvePaiement.REJETEE);
        preuve.setMotifRejet(motifRejet);
        return save(preuve);
    }
}
