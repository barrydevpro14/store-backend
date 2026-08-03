ALTER TABLE ligne_commande_vente
    ADD COLUMN IF NOT EXISTS date_ajout DATE;

ALTER TABLE commande_vente DROP CONSTRAINT IF EXISTS commande_vente_statut_check;
ALTER TABLE commande_vente
    ADD CONSTRAINT commande_vente_statut_check
        CHECK (statut IN ('DRAFT', 'VALIDATE', 'CLOTURE', 'CANCEL'));
