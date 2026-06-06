-- ── V39 — global system suppliers: entreprise_id nullable ────────────────────
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.columns
        WHERE table_name = 'fournisseur'
          AND column_name = 'entreprise_id'
          AND is_nullable = 'NO'
    ) THEN
        ALTER TABLE fournisseur ALTER COLUMN entreprise_id DROP NOT NULL;
    END IF;
END
$$;
