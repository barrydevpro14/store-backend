-- Supprime la redondance montant_total / montant_paye sur commande_vente.
-- Les montants vivent desormais uniquement sur facture_client (qui porte deja
-- montant_total + montant_paye + statut derive). La relation 1:1 commande<->facture
-- est garantie par F-V3 (chaque vente cree obligatoirement sa facture).
-- F-V5 (paiement echelonne) appellera factureClientDomainService.applyPaiement
-- sans plus propager le montantPaye sur la commande.

ALTER TABLE commande_vente DROP COLUMN montant_total;
ALTER TABLE commande_vente DROP COLUMN montant_paye;
