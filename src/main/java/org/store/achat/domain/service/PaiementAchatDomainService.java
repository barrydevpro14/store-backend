package org.store.achat.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.store.achat.application.dto.PaiementAchatCreate;
import org.store.achat.application.dto.PaiementAchatResponse;
import org.springframework.stereotype.Service;
import org.store.achat.domain.model.PaiementAchat;
import org.store.achat.domain.repository.PaiementAchatRepository;
import org.store.common.service.GlobalService;

import java.util.UUID;

@Service
public class PaiementAchatDomainService extends GlobalService<PaiementAchat, PaiementAchatRepository> {
    public PaiementAchatDomainService(PaiementAchatRepository repository) {
        super(repository);
    }

    /** Crée et persiste un paiement à partir d'un record groupé. */
    public PaiementAchat create(PaiementAchatCreate paiementAchatCreate) {
        PaiementAchat paiement = new PaiementAchat();
        paiement.setFacture(paiementAchatCreate.facture());
        paiement.setMontant(paiementAchatCreate.montant());
        paiement.setDatePaiement(paiementAchatCreate.datePaiement());
        paiement.setMoyen(paiementAchatCreate.moyen());
        return save(paiement);
    }

    public Page<PaiementAchatResponse> findResponsesByFactureId(UUID factureId, Pageable pageable) {
        return repository.findResponsesByFactureId(factureId, pageable);
    }
}
