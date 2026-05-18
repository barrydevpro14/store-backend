package org.store.achat.application.dto;

import org.store.achat.domain.model.LigneCommandeAchat;
import org.store.produit.application.dto.ProductSummaryResponse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.UUID;

public record LigneCommandeAchatResponse(
        UUID id,
        ProductSummaryResponse produit,
        FournisseurSummaryResponse fournisseur,
        int quantite,
        BigDecimal prixAchat,
        BigDecimal prixVente,
        BigDecimal montantLigne,
        String numeroLot,
        LocalDate dateExpiration
) {
    public LigneCommandeAchatResponse(LigneCommandeAchat ligne) {
        this(
                ligne.getId(),
                new ProductSummaryResponse(ligne.getProductFournisseur().getProduct()),
                new FournisseurSummaryResponse(ligne.getProductFournisseur().getFournisseur()),
                ligne.getQuantite(),
                ligne.getPrixAchat(),
                ligne.getPrixVente(),
                ligne.getPrixAchat().multiply(BigDecimal.valueOf(ligne.getQuantite())),
                ligne.getNumeroLot(),
                ligne.getDateExpiration()
        );
    }
}
