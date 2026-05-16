-- Correction typo historique sur facture_client : date_echeache -> date_echeance.
-- Les autres tables du schema (echeance, facture_achat, depense) utilisaient deja
-- l'orthographe correcte des V1. On aligne facture_client.
--
-- Backfill garde-fou : les factures historiques sans date d'echeance prennent la
-- date de la facture (cas comptant). Ensuite NOT NULL est applique.

ALTER TABLE facture_client RENAME COLUMN date_echeache TO date_echeance;

UPDATE facture_client SET date_echeance = date WHERE date_echeance IS NULL;

ALTER TABLE facture_client ALTER COLUMN date_echeance SET NOT NULL;
