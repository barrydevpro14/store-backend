-- First, ensure all non-compliant statuses are converted to FACTURE_GENEREE
UPDATE paiement_abonnement
SET statut = 'FACTURE_GENEREE'
WHERE statut NOT IN ('FACTURE_GENEREE', 'EN_RETARD', 'VALIDE');

-- Drop the old constraint and add the new one
ALTER TABLE paiement_abonnement
    DROP CONSTRAINT IF EXISTS paiement_abonnement_statut_check;
ALTER TABLE paiement_abonnement
    ADD CONSTRAINT paiement_abonnement_statut_check
        CHECK (statut IN ('FACTURE_GENEREE', 'EN_RETARD', 'VALIDE'));

-- Drop the columns that were migrated to preuve_paiement
ALTER TABLE paiement_abonnement DROP COLUMN moyen_id;
ALTER TABLE paiement_abonnement DROP COLUMN reference_transaction;
ALTER TABLE paiement_abonnement DROP COLUMN preuve_id;
ALTER TABLE paiement_abonnement DROP COLUMN motif_rejet;
