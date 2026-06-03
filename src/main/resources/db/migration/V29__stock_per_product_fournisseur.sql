-- Restructure stock to track quantity per ProductFournisseur (product × supplier × quality)
-- instead of per product only. This allows showing quality in the stock table
-- and prevents aggregation across different qualities.
--
-- Dev environment: existing stock rows are deleted and will be re-seeded by DemoProductSeeder.

DELETE FROM mouvement_stock;
DELETE FROM sortie_stock;
DELETE FROM entree_stock;
DELETE FROM stock;

ALTER TABLE stock DROP CONSTRAINT IF EXISTS uk_stock_magasin_produit;
ALTER TABLE stock DROP COLUMN IF EXISTS produit_id;

ALTER TABLE stock ADD COLUMN product_fournisseur_id UUID NOT NULL
    REFERENCES product_fournisseur(id);

ALTER TABLE stock ADD CONSTRAINT uk_stock_magasin_pf
    UNIQUE (magasin_id, product_fournisseur_id);
