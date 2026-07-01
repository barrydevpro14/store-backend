-- ── V52 — extend mouvement_stock.type CHECK to include ENTREE_INITIAL ────────
-- ENTREE_INITIAL covers manual stock entries recorded without a CommandeAchat
-- (used when a store boots the app with pre-existing physical stock).
DO $$
BEGIN
    IF EXISTS (
        SELECT 1 FROM pg_constraint
        WHERE conname = 'mouvement_stock_type_check'
    ) THEN
        ALTER TABLE mouvement_stock DROP CONSTRAINT mouvement_stock_type_check;
    END IF;

    ALTER TABLE mouvement_stock
        ADD CONSTRAINT mouvement_stock_type_check
        CHECK (type IN (
            'ENTREE_ACHAT',
            'ENTREE_INITIAL',
            'SORTIE_VENTE',
            'INVENTAIRE',
            'AJUSTEMENT',
            'RETOUR_CLIENT',
            'RETOUR_FOURNISSEUR'
        ));
END
$$;
