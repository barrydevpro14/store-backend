-- Stock = vue agregee par (magasin, produit). Le fournisseur est porte par EntreeStock (granularite lot pour le FIFO).
-- Suppression du champ product_fournisseur_id sur stock (sans usage) et garantie d'unicite (magasin, produit).

ALTER TABLE stock
    ALTER COLUMN magasin_id SET NOT NULL;

ALTER TABLE stock
    ALTER COLUMN produit_id SET NOT NULL;

ALTER TABLE stock
    DROP COLUMN IF EXISTS product_fournisseur_id;

ALTER TABLE stock
    ADD CONSTRAINT uk_stock_magasin_produit UNIQUE (magasin_id, produit_id);

CREATE INDEX idx_stock_produit ON stock (produit_id);
