-- Détachement de la clôture du statut : boolean editable indépendant
-- Les commandes CLOTURE (= validées + entièrement payées) deviennent VALIDATE avec editable=false

ALTER TABLE commande_vente
    ADD COLUMN IF NOT EXISTS editable BOOLEAN NOT NULL DEFAULT TRUE;

UPDATE commande_vente
SET statut   = 'VALIDATE',
    editable = FALSE
WHERE statut = 'CLOTURE';

ALTER TABLE commande_vente DROP CONSTRAINT IF EXISTS commande_vente_statut_check;
ALTER TABLE commande_vente
    ADD CONSTRAINT commande_vente_statut_check
        CHECK (statut IN ('DRAFT', 'VALIDATE', 'CANCEL'));
