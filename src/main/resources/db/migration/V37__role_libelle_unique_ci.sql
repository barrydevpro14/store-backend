-- ── V37 — case-insensitive uniqueness of role libelle per entreprise ─────────
-- Replace the case-sensitive UNIQUE constraint added in V36 with a
-- case-insensitive partial unique index on LOWER(libelle).

ALTER TABLE role DROP CONSTRAINT IF EXISTS uq_role_libelle_entreprise;

DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM pg_indexes
        WHERE indexname = 'uq_role_libelle_ci_entreprise'
    ) THEN
        CREATE UNIQUE INDEX uq_role_libelle_ci_entreprise
            ON role (LOWER(libelle), entreprise_id) NULLS NOT DISTINCT;
    END IF;
END
$$;
