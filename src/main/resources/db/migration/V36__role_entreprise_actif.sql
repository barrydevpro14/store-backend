-- ── V36 — per-company custom roles ──────────────────────────────────────────
-- 1. Add entreprise_id (nullable FK): NULL = system/global role
-- 2. Add actif (default true)
-- 3. Enforce uniqueness per (libelle, entreprise_id)

ALTER TABLE role
    ADD COLUMN entreprise_id UUID,
    ADD COLUMN actif         BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE role
    ADD CONSTRAINT fk_role_entreprise
        FOREIGN KEY (entreprise_id) REFERENCES entreprise (id) ON DELETE CASCADE;

CREATE INDEX idx_role_entreprise ON role (entreprise_id);

-- Uniqueness: two companies may reuse the same label; global roles must be unique
ALTER TABLE role
    ADD CONSTRAINT uq_role_libelle_entreprise
        UNIQUE NULLS NOT DISTINCT (libelle, entreprise_id);
