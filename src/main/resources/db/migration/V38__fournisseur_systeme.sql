-- ── V38 — système flag on fournisseur ────────────────────────────────────────
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'fournisseur' AND column_name = 'systeme'
    ) THEN
        ALTER TABLE fournisseur ADD COLUMN systeme BOOLEAN NOT NULL DEFAULT FALSE;
    END IF;
END
$$;
