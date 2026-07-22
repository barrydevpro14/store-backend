-- V62: GIN trigram indexes for product selector search (LIKE '%term%')
-- pg_trgm supports leading-wildcard patterns that B-tree cannot accelerate.

CREATE EXTENSION IF NOT EXISTS pg_trgm;

CREATE INDEX IF NOT EXISTS idx_product_nom_trgm
    ON product USING GIN (LOWER(nom) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_product_reference_trgm
    ON product USING GIN (LOWER(reference) gin_trgm_ops);

CREATE INDEX IF NOT EXISTS idx_category_product_libelle_trgm
    ON category_product USING GIN (LOWER(libelle) gin_trgm_ops);
