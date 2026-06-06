-- V15 — Achat module : folded reception into validate. The CommandeAchatStatut enum
-- keeps only DRAFT, RECEPTIONNEE, ANNULEE. Backfill any historical row carrying one
-- of the two dropped values to RECEPTIONNEE (they are semantically the closest match
-- now that validate materializes stock + facture in one transaction), then rebuild
-- the CHECK constraint with the narrowed allowed set.

UPDATE commande_achat
   SET statut = 'RECEPTIONNEE'
 WHERE statut IN ('VALIDEE', 'PARTIELLEMENT_RECEPTIONNEE');

ALTER TABLE commande_achat
    DROP CONSTRAINT IF EXISTS commande_achat_statut_check;

ALTER TABLE commande_achat
    ADD CONSTRAINT commande_achat_statut_check
    CHECK (statut IN ('DRAFT', 'RECEPTIONNEE', 'ANNULEE'));
