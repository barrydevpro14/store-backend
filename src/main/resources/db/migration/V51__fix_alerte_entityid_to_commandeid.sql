-- Backfill alerte.entity_id for FACTURE_*_OVERDUE alerts created before the
-- AlertScheduler fix that switched the persisted id from facture.id to commande.id.
-- Frontend uses entity_id as commande.id when opening the details dialog.

UPDATE alerte
SET entity_id = fa.commande_id
FROM facture_achat fa
WHERE alerte.entity_id = fa.id
  AND alerte.type = 'FACTURE_ACHAT_OVERDUE';

UPDATE alerte
SET entity_id = fv.commande_id
FROM facture_vente fv
WHERE alerte.entity_id = fv.id
  AND alerte.type = 'FACTURE_VENTE_OVERDUE';
