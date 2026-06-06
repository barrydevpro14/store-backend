package org.store.inventaire.domain.service;

import org.springframework.stereotype.Service;
import org.store.common.service.GlobalService;
import org.store.inventaire.application.dto.RapportInventaireCommand;
import org.store.inventaire.application.dto.RapportInventaireResponse;
import org.store.inventaire.domain.enums.StatutRapport;
import org.store.inventaire.domain.model.Inventaire;
import org.store.inventaire.domain.model.RapportInventaire;
import org.store.inventaire.domain.repository.RapportInventaireRepository;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

@Service
public class RapportInventaireDomainService extends GlobalService<RapportInventaire, RapportInventaireRepository> {
    public RapportInventaireDomainService(RapportInventaireRepository repository) {
        super(repository);
    }

    /** Construit et persiste le rapport d'inventaire. Champs derives (ecart, benefice, status) calcules ici. */
    public RapportInventaire create(Inventaire inventaire, RapportInventaireCommand command) {
        BigDecimal ecart = command.montantPhysique().subtract(command.montantAutomatique());
        BigDecimal benefice = command.montantPhysique()
                .add(command.montantCaisse())
                .subtract(command.depense())
                .subtract(command.montantRoulement());

        RapportInventaire rapport = new RapportInventaire();
        rapport.setInventaire(inventaire);
        rapport.setMontantAutomatique(command.montantAutomatique());
        rapport.setMontantPhysique(command.montantPhysique());
        rapport.setEcart(ecart);
        rapport.setMontantCaisse(command.montantCaisse());
        rapport.setDepense(command.depense());
        rapport.setMontantRoulement(command.montantRoulement());
        rapport.setDateDebutPeriode(command.dateDebutPeriode());
        rapport.setDateFinPeriode(command.dateFinPeriode());
        rapport.setBenefice(benefice);
        rapport.setStatus(deduireStatut(benefice));
        return save(rapport);
    }

    /** Recherche projetee du rapport d'un inventaire, scopee entreprise. */
    public Optional<RapportInventaireResponse> findResponseByInventaireId(UUID inventaireId, UUID entrepriseId) {
        return repository.findResponseByInventaireId(inventaireId, entrepriseId);
    }

    /** Verifie qu'un rapport n'a pas deja ete produit pour l'inventaire (idempotence cloture). */
    public boolean existsByInventaireId(UUID inventaireId) {
        return repository.existsByInventaireId(inventaireId);
    }

    /** Statut comptable du rapport selon le signe du benefice. */
    public StatutRapport deduireStatut(BigDecimal benefice) {
        int signe = benefice.signum();
        if (signe > 0) {
            return StatutRapport.BENEFICE;
        }
        if (signe < 0) {
            return StatutRapport.PERTE;
        }
        return StatutRapport.EQUILIBRE;
    }
}
