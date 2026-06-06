package org.store.vente.domain.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.vente.application.dto.CaisseResumeFilter;
import org.store.vente.application.dto.PaiementParMoyenResponse;
import org.store.vente.application.dto.PaiementVenteCreate;
import org.store.vente.application.dto.PaiementVenteResponse;
import org.store.vente.domain.model.PaiementVente;
import org.store.vente.domain.repository.PaiementVenteRepository;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
public class PaiementVenteDomainService extends GlobalService<PaiementVente, PaiementVenteRepository> {
    public PaiementVenteDomainService(PaiementVenteRepository repository) {
        super(repository);
    }

    /** Crée et persiste un paiement client à partir d'un record groupé (la date doit être résolue côté service applicatif). */
    public PaiementVente create(PaiementVenteCreate paiementVenteCreate) {
        PaiementVente paiement = new PaiementVente();
        paiement.setFacture(paiementVenteCreate.facture());
        paiement.setMontant(paiementVenteCreate.montant());
        paiement.setMoyen(paiementVenteCreate.moyen());
        paiement.setDatePaiement(paiementVenteCreate.datePaiement());
        return save(paiement);
    }

    public List<PaiementVente> findAllByFactureId(UUID factureId) {
        return repository.findAllByFactureId(factureId);
    }

    /** Listing paginé des paiements d'une facture, scopé entreprise (sécurité multi-tenant). */
    public Page<PaiementVenteResponse> findResponsesByFactureId(UUID factureId, UUID entrepriseId, Pageable pageable) {
        return repository.findResponsesByFactureId(factureId, entrepriseId, pageable);
    }

    /** Somme des paiements encaissés dans le magasin sur la période du range (tiroir-caisse). */
    public BigDecimal sumPaiementsForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.sumMontantByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }

    /** Ventilation des paiements groupés par moyen (CASH, WAVE, OM, CARD) avec total et nombre par moyen. */
    public List<PaiementParMoyenResponse> ventilationParMoyenForCaisse(CaisseResumeFilter range, UUID entrepriseId) {
        return repository.ventilationParMoyenByMagasinAndDay(range.magasinId(), entrepriseId, range.startOfPeriod(), range.endOfPeriod());
    }
}
