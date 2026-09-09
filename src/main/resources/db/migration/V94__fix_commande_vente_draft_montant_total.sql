-- Corrects commande_vente.montant_total on existing DRAFT sales orders left inconsistent by the
-- line-deletion bug (VenteServiceImpl.deleteLigne removed the entity without also removing it from
-- CommandeVente.lignes, so cascade=ALL silently revived the row at flush — the row survived while
-- the running total had already been decremented). Recomputes montant_total as the true sum of the
-- order's current lines so both are back in sync.
UPDATE commande_vente cv
SET montant_total = COALESCE((
    SELECT SUM(lcv.montant_total)
    FROM ligne_commande_vente lcv
    WHERE lcv.commande_id = cv.id
), 0)
WHERE cv.statut = 'DRAFT';
