package org.store.vente.domain.service;

import org.springframework.stereotype.Service;
import org.store.achat.domain.enums.MoyenPaiement;
import org.store.common.service.GlobalService;
import org.store.vente.domain.model.FactureClient;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.repository.PaiementVenteRepository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

@Service
public class PaiementVenteDomainService extends GlobalService<PaiementVente, PaiementVenteRepository> {
    public PaiementVenteDomainService(PaiementVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste un paiement client lié à une facture, à la date du jour si non précisée. */
    public PaiementVente create(FactureClient facture, BigDecimal montant, MoyenPaiement moyen) {
        PaiementVente paiement = new PaiementVente();
        paiement.setFacture(facture);
        paiement.setMontant(montant);
        paiement.setMoyen(moyen);
        paiement.setDatePaiement(LocalDate.now());
        return save(paiement);
    }

    public List<PaiementVente> findAllByFactureId(UUID factureId) {
        return repository.findAllByFactureId(factureId);
    }
}
