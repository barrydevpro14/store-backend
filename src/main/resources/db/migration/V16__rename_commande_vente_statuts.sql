-- Rename CommandeVente statuts: DELIVERED → VALIDATE, ANNULEE → CANCEL, drop NOT_DELIVERED
UPDATE commande_vente SET statut = 'VALIDATE' WHERE statut = 'DELIVERED';
UPDATE commande_vente SET statut = 'CANCEL'   WHERE statut = 'ANNULEE';
UPDATE commande_vente SET statut = 'DRAFT'    WHERE statut = 'NOT_DELIVERED';

ALTER TABLE commande_vente DROP CONSTRAINT IF EXISTS commande_vente_statut_check;
ALTER TABLE commande_vente
    ADD CONSTRAINT commande_vente_statut_check
        CHECK (statut IN ('DRAFT', 'VALIDATE', 'CANCEL'));
