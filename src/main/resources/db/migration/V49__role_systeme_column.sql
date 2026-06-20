-- Adds explicit systeme flag to role table.
-- Backfill: system roles have no entreprise (entreprise_id IS NULL).
ALTER TABLE role ADD COLUMN systeme BOOLEAN NOT NULL DEFAULT FALSE;
UPDATE role SET systeme = TRUE WHERE entreprise_id IS NULL;
