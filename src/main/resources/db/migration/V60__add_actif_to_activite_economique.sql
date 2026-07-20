-- Soft-delete support on activite_economique
ALTER TABLE activite_economique
    ADD COLUMN actif BOOLEAN NOT NULL DEFAULT TRUE;

-- Drop the global unique constraint to allow reuse of a deactivated libelle
ALTER TABLE activite_economique
    DROP CONSTRAINT IF EXISTS activite_economique_libelle_key;

-- Partial unique index: uniqueness enforced only among active records
CREATE UNIQUE INDEX uk_activite_economique_libelle_actif
    ON activite_economique (LOWER(libelle))
    WHERE actif = TRUE;
