-- Backfill created_at/updated_at for unites_mesure rows inserted before V96
-- added those columns (PIECE/SAC/KG/LITRE/METRE/METRE_CARRE/CARTON from
-- V73/V77, plus METRE_CUBE/SACHET/CENTIMETRE/MILLIMETRE/GRAMME from V95).
-- Without this, PostgreSQL's default NULLS FIRST ordering on
-- `ORDER BY created_at DESC` would surface every legacy/seeded unit ahead of
-- newly-created ones instead of behind them.

UPDATE unites_mesure
SET created_at = NOW(),
    updated_at = NOW()
WHERE created_at IS NULL;
