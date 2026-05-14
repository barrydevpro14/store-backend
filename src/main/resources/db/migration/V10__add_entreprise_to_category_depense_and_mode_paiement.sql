-- Scoping multi-tenant : chaque entreprise possede ses propres categories de depense.
-- Ajout d'un mode de paiement sur depense pour le rapprochement caisse a venir.

-- 1. CategoryDepense : ajout FK Entreprise + remplacement de l'unicite globale sur 'nom'
ALTER TABLE category_depense
    ADD COLUMN entreprise_id UUID NOT NULL;

ALTER TABLE category_depense
    ADD CONSTRAINT fk_category_depense_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise (id);

-- Drop dynamique de l'ancienne contrainte UNIQUE sur la seule colonne 'nom'.
DO $$
DECLARE
    cname TEXT;
BEGIN
    SELECT conname INTO cname
    FROM pg_constraint
    WHERE conrelid = 'category_depense'::regclass
      AND contype = 'u'
      AND array_length(conkey, 1) = 1
      AND conkey[1] = (SELECT attnum FROM pg_attribute
                       WHERE attrelid = 'category_depense'::regclass
                         AND attname = 'nom');
    IF cname IS NOT NULL THEN
        EXECUTE 'ALTER TABLE category_depense DROP CONSTRAINT ' || quote_ident(cname);
    END IF;
END $$;

ALTER TABLE category_depense
    ADD CONSTRAINT uk_category_depense_entreprise_nom UNIQUE (entreprise_id, nom);

CREATE INDEX idx_category_depense_entreprise ON category_depense (entreprise_id);

-- 2. Depense : ajout du mode de paiement (CASH/WAVE/OM/CARD)
ALTER TABLE depense
    ADD COLUMN mode_paiement VARCHAR(20) NOT NULL DEFAULT 'CASH';
